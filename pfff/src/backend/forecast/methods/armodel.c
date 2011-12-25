#include <time.h>
#include "forecast/methods/armodel.h"
#include "utils/memutils.h"
#include "nodes/value.h"
#include "nodes/parsenodes.h"
#include "nodes/nodes.h"
#include "catalog/pg_type.h"
#include "utils/array.h"
#include "catalog/pg_parameter.h"
#include <float.h>
#include <math.h>
#include "utils/guc.h"
#include "utils/portal.h"
#include "tcop/tcopprot.h"
#include "forecast/modelGraph/modelGraphReceiver.h"
#include "tcop/pquery.h"

//done
double arima_nonSeasonal_SMAPE(unsigned n, const double *x, double *grad, void *my_func_data) {
	ARModel 		*model=(ARModel*)my_func_data;
	int variable,phi,theta;
	int i,p,q,us;
	double oldf;
	double tempOverFlowCheck;
	double ga=1e-10;
	double f = 0.0;
	double fup = 0.0;
	double fdown = 0.0;
	double fval = 0.0;
	double *res = palloc0(model->obsCount*sizeof(double));




	double gradhelpup[n];
	double gradhelpdown[n];
	double valup[n];
	double valdown[n];
	double resBound=DBL_MAX/2;
		p=model->super.p;
	q=model->super.q;
	us=1;
	for(i=0; i<n; i++) {
		gradhelpup[i]=x[i]+ga;
		gradhelpdown[i]=x[i]-ga;
		valup[i]=0.0;
		valdown[i]=0.0;
	}

	f=0.0;
	for(i=(p); i<model->obsCount; i++) {
		fval=0.0;
		for(phi=0; phi<p; phi++) { //add ar part
			fval = fval +(x[phi] * (model->data[1][i-(phi)-1]));
		}
		for(theta=0; theta<q; theta++) { //add ma part
			fval = fval +(x[model->super.p+theta] * res[i-(theta)-1]);
		}
		if(model->super.includeConstant) //add Constant if enabled
			fval=fval+x[model->super.p+model->super.q];
		if(fval<0) fval=0; //we assume only positive forecast values are allowed
		res[i]=(fval-model->data[1][i]); //residuum
		//We have to restrict errorterms or the Gradient may become Infinity/Nan
		if(res[i]>resBound) {
			res[i]=resBound;
			elog (WARNING,"resbound breach detected. Codekey:ar_ns_smape1");
		}

			oldf=f;
			tempOverFlowCheck=((fabs(fval-model->data[1][i]))/(fval+model->data[1][i]));
			f=f+tempOverFlowCheck; //add square error

		if((tempOverFlowCheck>0 && oldf>f) ||(tempOverFlowCheck<0 && oldf<f)  ){
			f=DBL_MAX; //Overflow happend
			elog (WARNING,"Overflow detected. Codekey:ar_ns_smape2");
		}
		//calculate gradientpart for this obersvation
		for(variable=0; variable<model->super.p; variable++) { //ar-Gradients
			fup=fval-(x[variable]*(model->data[1][i-(variable)-1]))+(gradhelpup[variable]*(model->data[1][i-(variable)-1]));
			if(fup<0)
				fup=0; //we assume only positve forecasts are allowed
			fdown=fval-(x[variable]*(model->data[1][i-(variable)-1]))+(gradhelpdown[variable]*(model->data[1][i-(variable)-1]));
			if(fdown<0)
				fdown=0; //we assume only positve forecasts are allowed
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fabs(fup-(model->data[1][i])))/((fup+(model->data[1][i]))));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fabs(fdown-(model->data[1][i])))/((fdown+(model->data[1][i]))));
		}
		for(variable=p; variable<p+q; variable++) { //ma-Gradients
			fup=fval-(x[variable]*res[i-((variable-p))-1])+(gradhelpup[variable]*res[i-((variable-p))-1]);
			if(fup<0)
				fup=0; //we assume only positve forecasts are allowed
			fdown=fval-(x[variable]*res[i-((variable-p))-1])+(gradhelpdown[variable]*res[i-((variable-p))-1]);
			if(fdown<0)
				fdown=0;//we assume only positve forecasts are allowed
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fabs(fup-(model->data[1][i])))/((fup+(model->data[1][i]))));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fabs(fdown-(model->data[1][i])))/((fdown+(model->data[1][i]))));
		}
		//Constant Gradient
		if(model->super.includeConstant) {
			fup=fval-x[n-1] + gradhelpup[n-1];
			if(fup<0)
				fup=0; //we assume only positve forecasts are allowed
			fdown=fval-x[n-1] + gradhelpdown[n-1];
			if(fdown<0)
				fdown=0; //we assume only positve forecasts are allowed
			if((fup+model->data[1][i])!=0)
				valup[n-1]=valup[n-1]+((fabs(fup-(model->data[1][i])))/((fup+(model->data[1][i]))));
			if((fdown+model->data[1][i])!=0)
				valdown[n-1]=valdown[n-1]+((fabs(fdown-(model->data[1][i])))/((fdown+(model->data[1][i]))));
		}


	}
	//Some Optimizer are derivative-free, therefore we must check if the gradient field is malloc'ed
	if(grad!=NULL)
		for(variable=0; variable<n; variable++)
			grad[variable]= ((valup[variable])-(valdown[variable]))/(2*ga);

	//save current error terms, since we could be terminated any time
	for(variable=0; variable<(q); variable++) {
		model->super.uhat[variable]=res[model->obsCount-variable-1];
	}
	pfree(res);
	return (f/model->obsCount);
}

//done
double arima_nonSeasonal_CSS(unsigned n, const double *x, double *grad, void *my_func_data) {
	ARModel 		*model=(ARModel*)my_func_data;
	int p=model->super.p,q=model->super.q;
	int variable,phi,theta;
	int i;
	double ga=1e-10;
	double f = 0.0;
	double fup = 0.0;
	double fdown = 0.0;
	double fval = 0.0;
	double oldf;
	double tempOverFlowCheck;
	double *res = palloc(model->obsCount*sizeof(double));



	double gradhelpup[n];
	double gradhelpdown[n];
	double resbound=DBL_MAX/2;
	double valup[n];
	double valdown[n];
	
	for(i=0; i<model->obsCount; i++)
		res[i]=0.0;
	for(i=0; i<n; i++) {
		gradhelpup[i]=x[i]+ga;
		gradhelpdown[i]=x[i]-ga;
		valup[i]=0.0;
		valdown[i]=0.0;
	}


	f=0.0;
	//elog(INFO,"%i,%i",p,model->obsCount);
	for(i=(p); i<model->obsCount; i++) {
		fval=0.0;
		for(phi=0; phi<p; phi++) { //add ar part
			fval = fval +(x[phi] * (model->data[1][i-(phi)-1]));
		}
		for(theta=0; theta<q; theta++) { //add ma part
			fval = fval +(x[p+theta] * res[i-(theta)-1]);
		}
		if(model->super.includeConstant)
			fval=fval+x[n-1];
		res[i]=(fval-model->data[1][i]);
		//We have to restrict errorterms otherwise the Gradient may become Infinity/Nan. TODO: Better way to restrict the gradient
		if(res[i]>resbound) {
			res[i]=resbound;
			elog (WARNING,"resbound breach detected. Codekey:ar_ns_css1");
		}
		oldf=f;
		tempOverFlowCheck=((fval-model->data[1][i])*(fval-model->data[1][i]));
		f=f+tempOverFlowCheck; //add square error
		//////elog(INFO,"%f",tempOverFlowCheck);
		if((tempOverFlowCheck>0 && oldf>f) ||(tempOverFlowCheck<0 && oldf<f)  ) {
			f=DBL_MAX; //Overflow happend
			elog (WARNING,"Overflow detected. Codekey:ar_ns_css2");
		}
		//calculate gradientpart for this obersvation
		for(variable=0; variable<p; variable++) { //ar-Gradients
			fup=fval-(x[variable]*(model->data[1][i-(variable)-1]))+(gradhelpup[variable]*(model->data[1][i-(variable)-1]));
			fdown=fval-(x[variable]*(model->data[1][i-(variable)-1]))+(gradhelpdown[variable]*(model->data[1][i-(variable)-1]));
			valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		for(variable=p; variable<p+q; variable++) { //ma-Gradients
			fup=fval-(x[variable]*res[i-((variable-p))-1])+(gradhelpup[variable]*res[i-((variable-p))-1]);
			fdown=fval-(x[variable]*res[i-((variable-p))-1])+(gradhelpdown[variable]*res[i-((variable-p))-1]);
			valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		//Constant Gradient
		if(model->super.includeConstant) {
			fup=fval-x[n-1] + gradhelpup[n-1];
			fdown=fval-x[n-1] + gradhelpdown[n-1];
			valup[n-1]=valup[n-1]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			valdown[n-1]=valdown[n-1]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));

		}

	}
	//Some Optimizer are derivative-free, therefore we must check if the gradient field is malloc'ed
	if(grad!=NULL)
		for(variable=0; variable<n; variable++)
			grad[variable]= (valup[variable]-valdown[variable])/(2*ga);

	//save current error terms, since we could be terminated any time
	for(variable=0; variable<q; variable++) {
		model->super.uhat[variable]=res[model->obsCount-variable-1];
	}

	pfree(res);
	return f;
}

//done
double arima_nonSeasonal_ML(unsigned n, const double *x, double *grad, void *my_func_data) {
	ARModel 		*model=(ARModel*)my_func_data;
	double *varianzArray = palloc(model->obsCount*sizeof(double));
	double varianz;
	int p=model->super.p,q=model->super.q;
	int variable,phi,theta;
	int i;
	double ga=1e-10;
	double result=0.0;
	double f = 0.0;
	double fup = 0.0;
	double fdown = 0.0;
	double fval = 0.0;
	double oldf;
	double tempOverFlowCheck;
	double *res = palloc(model->obsCount*sizeof(double));
	double *fvalrem = palloc(model->obsCount*sizeof(double));


double tempSum=0.0;
	double gradhelpup[n];
	double gradhelpdown[n];
	double resbound=DBL_MAX/2;
	double valup[n];
	double valdown[n];
	for(i=0; i<n; i++) {
		gradhelpup[i]=x[i]+ga;
		gradhelpdown[i]=x[i]-ga;
		valup[i]=0.0;
		valdown[i]=0.0;
	}
	for(i=0; i<model->obsCount; i++) {
		res[i]=0.0;
		fvalrem[i]=0.0;
	}

	varianzArray[((p)-1)]=0;
	f=0.0;
	for(i=(p); i<model->obsCount; i++) {
		fval=0.0;
		for(phi=0; phi<p; phi++) { //add ar part
			fval = fval +(x[phi] * (model->data[1][i-(phi)-1]));
		}
		for(theta=0; theta<q; theta++) { //add ma part
			fval = fval +(x[p+theta] * res[i-(theta)-1]);
		}
		if(model->super.includeConstant)
			fval=fval+x[n-1];
		fvalrem[i]=fval;
		res[i]=(fval-model->data[1][i]);
		varianzArray[i]=((varianzArray[i-1]*model->obsCount)+res[i]*res[i])/model->obsCount;
		//We have to restrict errorterms or the Gradient may become Infinity/Nan
		if(res[i]>resbound) {
			res[i]=resbound;
			elog (WARNING,"resbound breach detected. Codekey:ar_ns_ml1");
		}
		oldf=f;
		tempOverFlowCheck=((fval-model->data[1][i])*(fval-model->data[1][i]));
		f=f+tempOverFlowCheck; //add square error
		if((tempOverFlowCheck>0 && oldf>f) ||(tempOverFlowCheck<0 && oldf<f)  ) {
			f=DBL_MAX; //Overflow happend
			elog (WARNING,"Overflow detected. Codekey:ar_ns_ml1");
		}
		//calculate gradientpart for this obersvation
		for(variable=0; variable<p; variable++) { //ar-Gradients
			fup=fval-(x[variable]*(model->data[1][i-(variable)])-1)+(gradhelpup[variable]*(model->data[1][i-(variable)-1]));
			fdown=fval-(x[variable]*(model->data[1][i-(variable)-1]))+(gradhelpdown[variable]*(model->data[1][i-(variable)-1]));
			valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		for(variable=p; variable<p+q; variable++) { //ma-Gradients
			fup=fval-(x[variable]*res[i-((variable-p))]-1)+(gradhelpup[variable]*res[i-((variable-p))-1]);
			fdown=fval-(x[variable]*res[i-((variable-p))-1])+(gradhelpdown[variable]*res[i-((variable-p))-1]);
			valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		//Constant Gradient
		if(model->super.includeConstant) {
			fup=fval-x[n-1] + gradhelpup[n-1];
			fdown=fval-x[n-1] + gradhelpdown[n-1];
			valup[n-1]=valup[n-1]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			valdown[n-1]=valdown[n-1]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));

		}

	}
	varianz=f/(model->obsCount-p-q);
	result=log((f/model->obsCount));
	
	for(i=(p); i<model->obsCount; i++) {
		tempSum+=varianzArray[i]/varianz;
	}
	result=result+(tempSum/model->obsCount);

	//calc gradvariance
	for(i=(p); i<model->obsCount; i++) {
		for(variable=0; variable<n; variable++) {
			valup[variable]=0;
			valdown[variable]=0;
			if(variable<p || variable==n-1) { //phi part
				for(theta=0; theta<q; theta++) { //add ma part
					valup[variable] = valup[variable] + res[i-(theta)-1];
					valdown[variable] = valup[variable] + res[i-(theta)-1];
				}
			} else if(variable <n-1) { //ma part
				for(theta=0; theta<q; theta++) { //add ma part
					if(theta!=variable) {
						valup[variable] = valup[variable] + res[i-(theta)-1];
						valdown[variable] = valup[variable] + res[i-(theta)-1];
					} else {
						fup=fvalrem[i]-(x[variable]*res[i-((variable-p))-1])+(gradhelpup[variable]*res[i-((variable-p))-1]);
						fdown=fvalrem[i]-(x[variable]*res[i-((variable-p))-1])+(gradhelpdown[variable]*res[i-((variable-p))-1]);
						valup[variable] = valup[variable] + fup-model->data[1][i];
						valdown[variable] = valdown[variable] + fdown-model->data[1][i];
					}
				}
			}
		}
	}


	//Some Optimizer are derivative-free, therefore we must check if the gradient field is malloc'ed
	if(grad!=NULL)
		for(variable=0; variable<n; variable++) {

			valup[variable]=valup[variable]/model->obsCount;
			valdown[variable]=valdown[variable]/model->obsCount;
			valup[variable]+=log(valup[variable]/model->obsCount);
			valdown[variable]+=log(valdown[variable]/model->obsCount);
			grad[variable]= (valup[variable]-valdown[variable])/(2*ga);
		}

	//save current error terms, since we could be terminated any time
	for(variable=0; variable<q; variable++) {
		model->super.uhat[variable]=res[model->obsCount-variable-1];
	}

	pfree(varianzArray);
	pfree(res);
	pfree(fvalrem);
	return result;
}

//done
double arima_AddSeasonal_SMAPE(unsigned n, const double *x, double *grad, void *my_func_data) {
	ARModel 		*model=(ARModel*)my_func_data;
	int variable,phi,theta;
	int i,us;
	double oldf;
	double tempOverFlowCheck;
	double ga=1e-10;
	double f = 0.0;
	double fup = 0.0;
	double fdown = 0.0;
	double fvalns = 0.0;
	double fvals = 0.0;
	double fvalsAns = 0.0;
	double *res = palloc(model->obsCount*sizeof(double));
 


	double gradhelpup[n];
	double gradhelpdown[n];
	double valup[n];
	double valdown[n];
	double resBound=DBL_MAX/2;
	
		us=1;

	for(i=0; i<model->obsCount; i++)
		res[i]=0.0;
	for(i=0; i<n; i++) {
		gradhelpup[i]=x[i]+ga;
		gradhelpdown[i]=x[i]-ga;
		valup[i]=0.0;
		valdown[i]=0.0;
	}

	f=0.0;
	for(i=Max(1*model->super.p,model->super.sp*model->sflag)	; i<model->obsCount; i++) {
		fvalns=0.0;
		fvals=0.0;
		for(phi=0; phi<model->super.p; phi++) { //add nonseasonal ar part
			fvalns = fvalns +(x[phi] * (model->data[1][i-(phi)-1]));
		}
		for(phi=0; phi<model->super.sp; phi++) { //add seasonal ar part
			fvals = fvals +(x[model->super.p+phi] * (model->data[1][i-(model->sflag*phi)-model->sflag]));
		}
		for(theta=0; theta<model->super.q; theta++) { //add nonseaosnal ma part
			fvalns = fvalns +(x[model->super.p+model->super.sp+theta] * res[i-(1*theta)-1]);
		}
		for(theta=0; theta<model->super.sq; theta++) { //add seasonal ma part
			fvals = fvals +(x[model->super.p+model->super.sp+model->super.q+theta] * res[i-(model->sflag*theta)-model->sflag]);
		}
		fvalsAns=fvalns+fvals;
		if(model->super.includeConstant) //add Constant if enabled
			fvalsAns=fvalsAns+x[n-1];
		if(fvalsAns<0) fvalsAns=0; //we assume only positive forecast values are allowed
		res[i]=(fvalsAns-model->data[1][i]);
		//We have to restrict errorterms or the Gradient may become Infinity/Nan
		if(res[i]>resBound) {
			res[i]=resBound;
			elog (WARNING,"resbound breach detected. Codekey:ar_s_smape1");
		}
		if((fvalsAns+model->data[1][i])!=0) { //maybe Forecast and true Value ar 0?
			oldf=f;
			tempOverFlowCheck=((fabs(fvalsAns-model->data[1][i]))/(fvalsAns+model->data[1][i]));
			f=f+tempOverFlowCheck; //add square error
		}
		if((tempOverFlowCheck>0 && oldf>f) ||(tempOverFlowCheck<0 && oldf<f)  ){
			f=DBL_MAX; //Overflow happend
			elog (WARNING,"Overflow detected. Codekey:ar_ss_smape2");
		}
		//calculate gradientpart for this obersvation
		for(variable=0; variable<model->super.p; variable++) { //nonseasonal ar-Gradients
			fup=fvalsAns-(x[variable]*(model->data[1][i-(1*variable)-1]))+(gradhelpup[variable]*(model->data[1][i-(1*variable)-1]));
			if(fup<0)
				fup=0; //we assume only positve forecasts are allowed
			fdown=fvalsAns-(x[variable]*(model->data[1][i-(1*variable)-1]))+(gradhelpdown[variable]*(model->data[1][i-(1*variable)-1]));
			if(fdown<0)
				fdown=0; //we assume only positve forecasts are allowed
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fabs(fup-(model->data[1][i])))/((fup+(model->data[1][i]))));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fabs(fdown-(model->data[1][i])))/((fdown+(model->data[1][i]))));
		}
		for(variable=model->super.p; variable<model->super.p+model->super.sp; variable++) { //seasonal ar-Gradients
			fup=fvalsAns-(x[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]))+(gradhelpup[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]));
			if(fup<0)
				fup=0; //we assume only positve forecasts are allowed
			fdown=fvalsAns-(x[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]))+(gradhelpdown[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]));
			if(fdown<0)
				fdown=0; //we assume only positve forecasts are allowed
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fabs(fup-(model->data[1][i])))/((fup+(model->data[1][i]))));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fabs(fdown-(model->data[1][i])))/((fdown+(model->data[1][i]))));
		}
		for(variable=model->super.p+model->super.sp; variable<model->super.p+model->super.sp+model->super.q; variable++) { //nonseasonal ma-Gradients
			fup=fvalsAns-(x[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1])+(gradhelpup[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1]);
			if(fup<0)
				fup=0; //we assume only positve forecasts are allowed
			fdown=fvalsAns-(x[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1])+(gradhelpdown[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1]);
			if(fdown<0)
				fdown=0;//we assume only positve forecasts are allowed
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fabs(fup-(model->data[1][i])))/((fup+(model->data[1][i]))));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fabs(fdown-(model->data[1][i])))/((fdown+(model->data[1][i]))));
		}
		for(variable=model->super.p+model->super.sp+model->super.q; variable<model->super.p+model->super.sp+model->super.q+model->super.sq; variable++) { //seasonal ma-Gradients
			fup=fvalsAns-(x[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag])+(gradhelpup[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag]);
			if(fup<0)
				fup=0; //we assume only positve forecasts are allowed
			fdown=fvalsAns-(x[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag])+(gradhelpdown[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag]);
			if(fdown<0)
				fdown=0;//we assume only positve forecasts are allowed
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fabs(fup-(model->data[1][i])))/((fup+(model->data[1][i]))));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fabs(fdown-(model->data[1][i])))/((fdown+(model->data[1][i]))));
		}
		
		//Constant Gradient
		if(model->super.includeConstant) {
			fup=fvalsAns-x[n-1] + gradhelpup[n-1];
			if(fup<0)
				fup=0; //we assume only positve forecasts are allowed
			fdown=fvalsAns-x[n-1] + gradhelpdown[n-1];
			if(fdown<0)
				fdown=0; //we assume only positve forecasts are allowed
			if((fup+model->data[1][i])!=0)
				valup[n-1]=valup[n-1]+((fabs(fup-(model->data[1][i])))/((fup+(model->data[1][i]))));
			if((fdown+model->data[1][i])!=0)
				valdown[n-1]=valdown[n-1]+((fabs(fdown-(model->data[1][i])))/((fdown+(model->data[1][i]))));
		}


	}
	//Some Optimizer are derivative-free, therefore we must check if the gradient field is malloc'ed
	if(grad!=NULL)
		for(variable=0; variable<n; variable++)
			grad[variable]= ((valup[variable]/model->obsCount)-(valdown[variable]/model->obsCount))/(2*ga);

	//save current error terms, since we could be terminated any time
	for(variable=0; variable<(Max(model->super.q,model->super.sq*model->sflag)); variable++) {
		model->super.uhat[variable]=res[model->obsCount-variable-1];
	}
	pfree(res);
	return (f/model->obsCount);
}

//done
double arima_AddSeasonal_CSS(unsigned n, const double *x, double *grad, void *my_func_data) {
	ARModel 		*model=(ARModel*)my_func_data;
	int variable,phi,theta;
	int i,us;
	double oldf;
	double tempOverFlowCheck;
	double ga=1e-10;
	double f = 0.0;
	double fup = 0.0;
	double fdown = 0.0;
	double fvalns = 0.0;
	double fvals = 0.0;
	double fvalsAns = 0.0;
	double *res = palloc(model->obsCount*sizeof(double));

		

	double gradhelpup[n];
	double gradhelpdown[n];
	double valup[n];
	double valdown[n];
	double resBound=DBL_MAX/2;
		us=1;

	for(i=0; i<model->obsCount; i++)
		res[i]=0.0;
	for(i=0; i<n; i++) {
		gradhelpup[i]=x[i]+ga;
		gradhelpdown[i]=x[i]-ga;
		valup[i]=0.0;
		valdown[i]=0.0;
	}

	f=0.0;
	for(i=Max(1*model->super.p,model->super.sp*model->sflag); i<model->obsCount; i++) {
		fvalns=0.0;
		fvals=0.0;
		for(phi=0; phi<model->super.p; phi++) { //add nonseasonal ar part
			fvalns = fvalns +(x[phi] * (model->data[1][i-(phi)-1]));
		}
		for(phi=0; phi<model->super.sp; phi++) { //add seasonal ar part
			fvals = fvals +(x[model->super.p+phi] * (model->data[1][i-(model->sflag*phi)-model->sflag]));
		}
		for(theta=0; theta<model->super.q; theta++) { //add nonseaosnal ma part
			fvalns = fvalns +(x[model->super.p+model->super.sp+theta] * res[i-(1*theta)-1]);
		}
		for(theta=0; theta<model->super.sq; theta++) { //add seasonal ma part
			fvals = fvals +(x[model->super.p+model->super.sp+model->super.q+theta] * res[i-(model->sflag*theta)-model->sflag]);
		}
		fvalsAns=fvalns+fvals;
		if(model->super.includeConstant) //add Constant if enabled
			fvalsAns=fvalsAns+x[n-1];
		res[i]=(fvalsAns-model->data[1][i]);
		//We have to restrict errorterms or the Gradient may become Infinity/Nan
		if(res[i]>resBound) {
			res[i]=resBound;
			elog (WARNING,"resbound breach detected. Codekey:ar_s_smape1");
		}
		if((fvalsAns+model->data[1][i])!=0) { //maybe Forecast and true Value ar 0?
			oldf=f;
			tempOverFlowCheck=((fvalsAns-model->data[1][i])*(fvalsAns-model->data[1][i]));
			//////elog(INFO,"%f",tempOverFlowCheck);
			f=f+tempOverFlowCheck; //add square error
		}
		if((tempOverFlowCheck>0 && oldf>f) ||(tempOverFlowCheck<0 && oldf<f)  ){
			f=DBL_MAX; //Overflow happend
			elog (WARNING,"Overflow detected. Codekey:ar_ss_smape2");
		}
		//calculate gradientpart for this obersvation
		for(variable=0; variable<model->super.p; variable++) { //nonseasonal ar-Gradients
			fup=fvalsAns-(x[variable]*(model->data[1][i-(1*variable)-1]))+(gradhelpup[variable]*(model->data[1][i-(1*variable)-1]));
			
			
			fdown=fvalsAns-(x[variable]*(model->data[1][i-(1*variable)-1]))+(gradhelpdown[variable]*(model->data[1][i-(1*variable)-1]));
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		for(variable=model->super.p; variable<model->super.p+model->super.sp; variable++) { //seasonal ar-Gradients
			
			fup=fvalsAns-(x[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]));
			fup=fup+(gradhelpup[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]));
			fdown=fvalsAns-(x[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]))+(gradhelpdown[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]));
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		for(variable=model->super.p+model->super.sp; variable<model->super.p+model->super.sp+model->super.q; variable++) { //nonseasonal ma-Gradients
			fup=fvalsAns-(x[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1])+(gradhelpup[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1]);
			fdown=fvalsAns-(x[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1])+(gradhelpdown[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1]);
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		for(variable=model->super.p+model->super.sp+model->super.q; variable<model->super.p+model->super.sp+model->super.q+model->super.sq; variable++) { //seasonal ma-Gradients
			fup=fvalsAns-(x[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag])+(gradhelpup[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag]);
			fdown=fvalsAns-(x[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag])+(gradhelpdown[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag]);
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		
		//Constant Gradient
		if(model->super.includeConstant) {
			fup=fvalsAns-x[n-1] + gradhelpup[n-1];
			fdown=fvalsAns-x[n-1] + gradhelpdown[n-1];
			valup[n-1]=valup[n-1]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			valdown[n-1]=valdown[n-1]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}


	}
	//Some Optimizer are derivative-free, therefore we must check if the gradient field is malloc'ed
	if(grad!=NULL)
		for(variable=0; variable<n; variable++)
			grad[variable]= ((valup[variable]/model->obsCount)-(valdown[variable]/model->obsCount))/(2*ga);

	//save current error terms, since we could be terminated any time
	for(variable=0; variable<(Max(model->super.q,model->super.sq*model->sflag)); variable++) {
		model->super.uhat[variable]=res[model->obsCount-variable-1];
	}
	pfree(res);
	return (f/model->obsCount);
}

//done
double arima_AddSeasonal_ML(unsigned n, const double *x, double *grad, void *my_func_data) {
	ARModel 		*model=(ARModel*)my_func_data;
	
	double varianz;

	int variable,phi,theta;
	int i;
	double ga=1e-10;
	double result=0.0;
	double f = 0.0;
	double fup = 0.0;
	double fdown = 0.0;
	double fvals = 0.0;
	double fvalns = 0.0;
	double fvalsAns = 0.0;
	double oldf;
	double tempOverFlowCheck;
	double *res = palloc0(model->obsCount*sizeof(double));
	double *fvalrem = palloc0(model->obsCount*sizeof(double));
	double *varianzArray = palloc0(model->obsCount*sizeof(double));

	double tempSum=0.0;
	double gradhelpup[n];
	double gradhelpdown[n];
	double resbound=DBL_MAX/2;
	double valup[n];
	double valdown[n];
	for(i=0; i<n; i++) {
		gradhelpup[i]=x[i]+ga;
		gradhelpdown[i]=x[i]-ga;
		valup[i]=0.0;
		valdown[i]=0.0;
	}

	varianzArray[Max(0,(Max(model->super.p,model->sflag*model->super.sp)-1))]=0;
	f=0.0;
		for(i=Max(1*model->super.p,model->super.sp*model->sflag); i<model->obsCount; i++) {
		fvalns=0.0;
		fvals=0.0;
		for(phi=0; phi<model->super.p; phi++) { //add nonseasonal ar part
			fvalns = fvalns +(x[phi] * (model->data[1][i-(phi)-1]));
		}
		for(phi=0; phi<model->super.sp; phi++) { //add seasonal ar part
			fvals = fvals +(x[model->super.p+phi] * (model->data[1][i-(model->sflag*phi)-model->sflag]));
		}
		for(theta=0; theta<model->super.q; theta++) { //add nonseaosnal ma part
			fvalns = fvalns +(x[model->super.p+model->super.sp+theta] * res[i-(1*theta)-1]);
		}
		for(theta=0; theta<model->super.sq; theta++) { //add seasonal ma part
			fvals = fvals +(x[model->super.p+model->super.sp+model->super.q+theta] * res[i-(model->sflag*theta)-model->sflag]);
		}
		fvalsAns=fvalns+fvals;
		if(model->super.includeConstant) //add Constant if enabled
			fvalsAns=fvalsAns+x[n-1];
		
		
		
		fvalrem[i]=fvalsAns;
		res[i]=(fvalsAns-model->data[1][i]);
		varianzArray[i]=((varianzArray[i-1]*model->obsCount)+res[i]*res[i])/model->obsCount;
		//We have to restrict errorterms or the Gradient may become Infinity/Nan
		if(res[i]>resbound) {
			res[i]=resbound;
			elog (WARNING,"resbound breach detected. Codekey:ar_ns_ml1");
		}
		oldf=f;
		tempOverFlowCheck=((fvalsAns-model->data[1][i])*(fvalsAns-model->data[1][i]));
		f=f+tempOverFlowCheck; //add square error
		if((tempOverFlowCheck>0 && oldf>f) ||(tempOverFlowCheck<0 && oldf<f)  ) {
			f=DBL_MAX; //Overflow happend
			elog (WARNING,"Overflow detected. Codekey:ar_ns_ml1");
		}
		//calculate gradientpart for this obersvation
		for(variable=0; variable<model->super.p; variable++) { //nonseasonal ar-Gradients
			fup=fvalsAns-(x[variable]*(model->data[1][i-(1*variable)-1]))+(gradhelpup[variable]*(model->data[1][i-(1*variable)-1]));
			
			
			fdown=fvalsAns-(x[variable]*(model->data[1][i-(1*variable)-1]))+(gradhelpdown[variable]*(model->data[1][i-(1*variable)-1]));
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		for(variable=model->super.p; variable<model->super.p+model->super.sp; variable++) { //seasonal ar-Gradients
			fup=fvalsAns-(x[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]))+(gradhelpup[variable]*(model->data[1][i-(model->sflag*variable-model->super.p)-model->sflag]));
			fdown=fvalsAns-(x[variable]*(model->data[1][i-(model->sflag*(variable-model->super.p))-model->sflag]))+(gradhelpdown[variable]*(model->data[1][i-(model->sflag*variable-model->super.p)-model->sflag]));
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		for(variable=model->super.p+model->super.sp; variable<model->super.p+model->super.sp+model->super.q; variable++) { //nonseasonal ma-Gradients
			fup=fvalsAns-(x[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1])+(gradhelpup[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1]);
			fdown=fvalsAns-(x[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1])+(gradhelpdown[variable]*res[i-(1*(variable-model->super.p-model->super.sp))-1]);
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		for(variable=model->super.p+model->super.sp+model->super.q; variable<model->super.p+model->super.sp+model->super.q+model->super.sq; variable++) { //seasonal ma-Gradients
			fup=fvalsAns-(x[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag])+(gradhelpup[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag]);
			fdown=fvalsAns-(x[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag])+(gradhelpdown[variable]*res[i-(model->sflag*(variable-model->super.p-model->super.sp-model->super.q))-model->sflag]);
			if((fup+model->data[1][i])!=0)
				valup[variable]=valup[variable]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			if((fdown+model->data[1][i])!=0)
				valdown[variable]=valdown[variable]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}
		
		//Constant Gradient
		if(model->super.includeConstant) {
			fup=fvalsAns-x[n-1] + gradhelpup[n-1];
			fdown=fvalsAns-x[n-1] + gradhelpdown[n-1];
			valup[n-1]=valup[n-1]+((fup-(model->data[1][i]))*(fup-(model->data[1][i])));
			valdown[n-1]=valdown[n-1]+((fdown-(model->data[1][i]))*(fdown-(model->data[1][i])));
		}

	}
	varianz=f/(model->obsCount-n);
	result=log((f/model->obsCount));

	for(i=Max(1*model->super.p,model->super.sp*model->sflag); i<model->obsCount; i++) {
		tempSum+=varianzArray[i]/varianz;
	}
	result=result+(tempSum/model->obsCount);

	//calc gradvariance
	for(i=Max(1*model->super.p,model->super.sp*model->sflag); i<model->obsCount; i++) {
		for(variable=0; variable<n; variable++) {
			valup[variable]=0;
			valdown[variable]=0;
			if(variable<model->super.p+model->super.sp || variable==n-1) { //phi part
				for(phi=0; phi<model->super.p; phi++) { //add ma part
					valup[variable] = valup[variable] + res[i-(phi)-1];
					valdown[variable] = valup[variable] + res[i-(phi)-1];
				}
				for(phi=model->super.p; phi<model->super.p+model->super.sp; phi++) { //add ma part
					valup[variable] = valup[variable] + res[i-(model->sflag*(phi-model->super.p))-model->sflag];
					valdown[variable] = valup[variable] + res[i-(model->sflag*(phi-model->super.p))-model->sflag];
				}
				
			} else if(variable <n-1) { //ma part
				for(theta=model->super.p+model->super.sp; theta<model->super.p+model->super.sp+model->super.q; theta++) { //add ma part
					if(theta!=variable) {
						valup[variable] = valup[variable] + res[i-(theta-(model->super.p+model->super.sp))-1];
						valdown[variable] = valup[variable] + res[i-(theta-(model->super.p+model->super.sp))-1];
					} else {
						fup=fvalrem[i]-(x[variable]*res[i-((variable-(model->super.p+model->super.sp)))-1])+(gradhelpup[variable]*res[i-((variable-(model->super.p+model->super.sp)))-1]);
						fdown=fvalrem[i]-(x[variable]*res[i-((variable-(model->super.p+model->super.sp)))-1])+(gradhelpdown[variable]*res[i-((variable-(model->super.p+model->super.sp)))-1]);
						valup[variable] = valup[variable] + fup-model->data[1][i];
						valdown[variable] = valdown[variable] + fdown-model->data[1][i];
					}
				}
				for(theta=model->super.p+model->super.sp+model->super.q; theta<model->super.p+model->super.sp+model->super.q+model->super.sq; theta++) { //add ma part
					if(theta!=variable) {
						valup[variable] = valup[variable] + res[i-(theta-(model->super.p+model->super.sp-model->super.q))-1];
						valdown[variable] = valup[variable] + res[i-(theta-(model->super.p+model->super.sp-model->super.q))-1];
					} else {
						fup=fvalrem[i]-(x[variable]*res[i-((variable-(model->super.p+model->super.sp-model->super.q)))-1])+(gradhelpup[variable]*res[i-((variable-(model->super.p+model->super.sp-model->super.q)))-1]);
						fdown=fvalrem[i]-(x[variable]*res[i-((variable-(model->super.p+model->super.sp-model->super.q)))-1])+(gradhelpdown[variable]*res[i-((variable-(model->super.p+model->super.sp-model->super.q)))-1]);
						valup[variable] = valup[variable] + fup-model->data[1][i];
						valdown[variable] = valdown[variable] + fdown-model->data[1][i];
					}
				}
			}
		}
	}


	//Some Optimizer are derivative-free, therefore we must check if the gradient field is malloc'ed
	if(grad!=NULL)
		for(variable=0; variable<n; variable++) {

			valup[variable]=valup[variable]/model->obsCount;
			valdown[variable]=valdown[variable]/model->obsCount;
			valup[variable]+=log(valup[variable]/model->obsCount);
			valdown[variable]+=log(valdown[variable]/model->obsCount);
			grad[variable]= (valup[variable]-valdown[variable])/(2*ga);
		}

		//save current error terms, since we could be terminated any time
	for(variable=0; variable<(Max(model->super.q,model->super.sq*model->sflag)); variable++) {
		model->super.uhat[variable]=res[model->obsCount-variable-1];
	}

	pfree(varianzArray);
	pfree(res);
	pfree(fvalrem);
	return result;
}

double arima_MulSeasonal_SMAPE(unsigned n, const double *x, double *grad, void *my_func_data) {
//	ARModel 		*model=(ARModel*)my_func_data;
	elog(ERROR,"not supported yet");
	return 0;
}


double arima_MulSeasonal_CSS(unsigned n, const double *x, double *grad, void *my_func_data) {
//	ARModel 		*model=(ARModel*)my_func_data;
	elog(ERROR,"not supported yet");
	return 0;
}


double arima_MulSeasonal_ML(unsigned n, const double *x, double *grad, void *my_func_data) {
	//ARModel 		*model=(ARModel*)my_func_data;
	elog(ERROR,"not supported yet");
	return 0;
}



//done
double
**expandTDDataArrayTo(double** array, int size,int v) {
	int i;
	for (i = 0; i < v; i++) {
		array[i] = (double *) repalloc(array[i], size
		                               * sizeof(**(array)));
	}
	return array;
}

void
initarModel(PG_FUNCTION_ARGS) {
	ARModel 		*m;
	MemoryContext 	old;
	ModelInfo		*model;


	model = (ModelInfo *)PG_GETARG_POINTER(0);

	m = makeNode(ARModel);

	/* create new MemoryContext for the data
	 * 1. it can get huge
	 * 2. it has a possible shorter lifespan than the parent context
	 */
	m->dataCtx = AllocSetContextCreate(CurrentMemoryContext,"ArData",ALLOCSET_DEFAULT_MINSIZE,ALLOCSET_DEFAULT_INITSIZE,MaxAllocSize);
	old = MemoryContextSwitchTo(m->dataCtx);

	// initialize data array
	m->data = palloc(2*sizeof(double*));
	m->data[0] = palloc(512*sizeof(double));
	m->data[1] = palloc(512*sizeof(double));
	m->maxObs = 512;
	m->obsCount = 0;

	MemoryContextSwitchTo(old);

	/* initialize standard values now much early in createplan*/
	m->super.p = 0;
	m->super.d = 0;
	m->super.seasonType=0;
	m->super.q = 0;
	m->super.constant[1] = 0.0;
	m->super.constant[0] = 0.0;
	m->super.includeConstant = 0;
	m->super.ycount = 0;
	m->super.sp=0;
	m->super.sd=0;
	m->super.sq=0;
	m->super.pd=0;
	m->super.errorfunction="SSE";
	m->super.phis=NULL;
	m->super.thetas=NULL;
	/* parse the Input Parameters. Be careful. Parameter list has to be set in ModelInfo */
	if(model->parameterList)
		parseArParameters(model->parameterList,&(m->super));

	model->model=(Model *)m;

}
void
parseArParameters(List *parameterList, ArimaModel *specificModel) {
	ListCell				*cell;
	
	foreach(cell,parameterList) {
		AlgorithmParameter		*param = lfirst(cell);

		/* parse maximum AR lag */
		if(strcmp(param->key,"ar") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->p = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"d") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->d = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"c") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->includeConstant = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"sar") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->sp = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"sma") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->sq = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"sd") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->sd = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"period") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->pd = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"error") == 0) {
			if(IsA(&(param->value->val),String)) {
				specificModel->errorfunction = strVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an String value"),
				         errposition(param->value->location)));
		} else if(strcmp(param->key,"ma") == 0) {
			if(IsA(&(param->value->val),Integer)) {
				specificModel->q = intVal(&param->value->val);
			} else
				ereport(ERROR,
				        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
				         errmsg("Parameter value has to be an Integer value"),
				         errposition(param->value->location)));
		} else
			ereport(ERROR,
			        (errcode(ERRCODE_INVALID_PARAMETER_VALUE),
			         errmsg("Parameter not known"),
			         errposition(((A_Const *)param->value)->location)));
	}
}

//done
void
processarModel(PG_FUNCTION_ARGS) {
	double 			value;
	ARModel		*model;
	MemoryContext	old;

	model = (ARModel *)PG_GETARG_POINTER(0);
	value = PG_GETARG_FLOAT8(1);

	old = MemoryContextSwitchTo(model->dataCtx);

	if (model->obsCount == model->maxObs) {
		// not enough space -> expand the data array
		model->maxObs *= 2;
		model->data = expandTDDataArrayTo(model->data,model->maxObs,2);
	}

	model->data[1][model->obsCount] = value;
	Assert(model->data[1][model->obsCount] == value);

	model->obsCount++;

	MemoryContextSwitchTo(old);

}

void
finalizearModel(PG_FUNCTION_ARGS) {
	MemoryContext 	old;
	
	int				i,j;
	int 			maxArCount;
	int retv; //returnValue for optimization
	nlopt_opt opt;
	nlopt_opt localopt;
	clock_t start, end;
	double *zs,*ubns,*lbns;
	double minf,diff;
	
	ARModel 		*model=(ARModel *)PG_GETARG_POINTER(0);
	old = MemoryContextSwitchTo(model->dataCtx);
	zs=(double*)palloc0(model->super.sp+model->super.sq+model->super.includeConstant+model->super.p+model->super.q);
	ubns=(double*)palloc0(model->super.p+model->super.q+model->super.sq+model->super.sp+model->super.includeConstant);
	lbns=(double*)palloc0(model->super.p+model->super.q+model->super.sq+model->super.sp+model->super.includeConstant);
	maxArCount=Max(model->super.p,(model->super.sp*model->super.pd)); //how many function values should be stored
	if(model->super.phis)
		pfree(model->super.y);
	model->super.y = palloc(maxArCount*sizeof(double));
	if(model->super.phis)
		pfree(model->super.uhat);
	model->super.uhat=palloc(((Max(model->super.q,(model->super.sq*model->super.pd))))*sizeof(double)); 
	if(!model->super.phis)
		model->super.phis = palloc0((model->super.p+model->super.sp)*sizeof(double));
	if(!model->super.thetas)
		model->super.thetas = palloc0((model->super.q+model->super.sq)*sizeof(double));
	memcpy(model->super.y,&(model->data[1][0])+model->obsCount-(maxArCount),(maxArCount)*sizeof(*model->super.y));  //store old Function values
	elog(INFO,"stored %i tuple",maxArCount);

	//non-seasonal diff
	for(i=0; i<model->super.d; i++)
		for(j=model->obsCount-1; j>0; j--)
			model->data[1][j]=model->data[1][j]-model->data[1][j-1];

	//seasonal diff
	for(i=0; i<model->super.sd; i++)
		for(j=model->obsCount-1; j>model->super.pd; j--)
			model->data[1][j]=model->data[1][j]-model->data[1][j-model->super.pd];

	//calculation non-seasonal Arima if needed
		model->sflag=model->super.pd; // we try to optimize the non-seasonal part,therefore we take 1 as Exponent of the Backshift Operator

		//To allow global optimization, we have to set bounds for the variables, open: how to set lower bound?
		for (i = 0; i < model->super.p+model->super.q+model->super.sq+model->super.sp; i += 1) {
			ubns[i]=1.0;
			lbns[i]=-5.0;
			zs[i]=1.0/(model->super.p+model->super.q+model->super.sq+model->super.sp);
		}

		//Constant does not have any restrictions, anyway most global optimization Algorithms need bounds,therefore we use 1e10 as a huge number. Larger Constants would cause numerical trouble anyway
		if(model->super.includeConstant) {
			ubns[model->super.p+model->super.sp+model->super.q+model->super.sq]=1e10;
			lbns[model->super.p+model->super.sp+model->super.q+model->super.sq]=-1e10;
			zs[model->super.p+model->super.sp+model->super.q+model->super.sq]=0;
		}
		
		if(model->super.phis[0]!=0){
			//reestimating
			for(i=0;i<model->super.p;i++)
				zs[i]=model->super.phis[i];
			for(i=0;i<model->super.sp;i++)
				zs[i+model->super.p]=model->super.phis[i+model->super.p];
			for(i=0;i<model->super.q;i++)
				zs[i+model->super.p+model->super.sp]=model->super.thetas[i];
			for(i=0;i<model->super.sq;i++)
				zs[i+model->super.p+model->super.sp+model->super.q]=model->super.thetas[i+model->super.q];
			if(model->super.includeConstant)
				zs[i+model->super.p+model->super.sp+model->super.q+model->super.sq]=model->super.constant[0];
		}

		opt = nlopt_create(getOptimMethod(optim_method_general), model->super.p+model->super.sp+model->super.q+model->super.sq+model->super.includeConstant);

		//as a local optimizer we use a vairant of Nelder Mead, maybe use a config Parameter later?
		localopt = nlopt_create(getOptimMethod(optim_method_local),  model->super.p+model->super.q+model->super.sq+model->super.sp+model->super.includeConstant);
		//some Algorithms dislike bounds
		if(optim_method_general!=8 &&optim_method_general!=9) {
			nlopt_set_upper_bounds(opt, ubns);
			nlopt_set_lower_bounds(opt, lbns);
		}
		nlopt_set_local_optimizer(opt,localopt);
		if(model->super.errorfunction==NULL) model->super.errorfunction="DEFAULT";
		if(model->super.sp+model->super.sq && model->super.seasonType==0)
		{
			if(strcmp(model->super.errorfunction,"SMAPE")==0)
				nlopt_set_min_objective(opt, arima_AddSeasonal_SMAPE, model);
			else if(strcmp(model->super.errorfunction,"ML")==0)
				nlopt_set_min_objective(opt, arima_AddSeasonal_ML, model);
			else
				nlopt_set_min_objective(opt, arima_AddSeasonal_CSS, model);
		}
		else if(!(model->super.sp+model->super.sq))
		{
			if(strcmp(model->super.errorfunction,"SMAPE")==0)
				nlopt_set_min_objective(opt, arima_nonSeasonal_SMAPE, model);
			else if(strcmp(model->super.errorfunction,"ML")==0)
				nlopt_set_min_objective(opt, arima_nonSeasonal_ML, model);
			else
				nlopt_set_min_objective(opt, arima_nonSeasonal_CSS, model);
		}
		else 
		{
			if(strcmp(model->super.errorfunction,"SMAPE")==0)
				nlopt_set_min_objective(opt, arima_MulSeasonal_SMAPE, model);
			else if(strcmp(model->super.errorfunction,"ML")==0)
				nlopt_set_min_objective(opt, arima_MulSeasonal_ML, model);
			else
				nlopt_set_min_objective(opt, arima_MulSeasonal_CSS, model);
		}

	if(optim_term_maxtime)
		nlopt_set_maxtime(opt, optim_term_maxtime);
	if(optim_term_ftol_abs)
		nlopt_set_ftol_abs(opt,optim_term_ftol_abs);
	if(optim_term_ftol_rel)
		nlopt_set_ftol_rel(opt,optim_term_ftol_rel);
	if(optim_term_xtol_rel)
		nlopt_set_xtol_rel(opt,optim_term_xtol_rel);
	if(optim_term_xtol_abs)
		nlopt_set_xtol_abs1(opt,optim_term_xtol_abs);
	if(optim_term_maxeval)
		nlopt_set_maxeval(opt,optim_term_maxeval);


		
		start=clock();
		
		retv=nlopt_optimize(opt, zs, &minf);
		end=clock();
		diff=(double)(end-start)/CLOCKS_PER_SEC;
	elog(INFO,"Optimization time: %f",diff);
		//This happens due to several reasons, anyway this is the only Output the LBFGS Routine gives us
		if (retv < 0 && optim_method_general==10) {
			elog(WARNING,"nlopt  of non seasonal part failed with code: %i, LBFGS routine cannot reach terminiation criteria, anyway the solution may be acceptable.. Current val of f was:%f",retv,minf);
		} else if(retv<0)
			elog(WARNING,"nlopt of non seasonal part  failed with code: %i, Current val of f was:%f",retv,minf);
		else {
			if(retv==6)
				elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with maxtime reached\n",  minf);
			else if(retv==5)
				elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with maxEval reached\n",  minf);
			else if(retv==4)
				elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with xtol reached\n", minf);
			else if(retv==3)
				elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with ftol reached\n", minf);
			else if(retv==2)
				elog(INFO,"found minimum for non-seasonal part at f = %0.10g and stopped with stopVal reached\n", minf);
			else if(retv==1)
				elog(INFO,"found minimum for non-seasonal part at f = %0.10gd\n", minf);
		}
		nlopt_destroy(opt);
		nlopt_destroy(localopt);

		//check solution, There are some effects can give hints about optimization problems
		for(i=0; i<model->super.p+model->super.q+model->super.sp+model->super.sq; i++) {

			if(zs[i]<1e-13 && retv==6) {
				elog(WARNING,"Please check solution for non-seasonal part, maybe you should rise term_maxtime or change the algorithm");
				break;
			}
			if(zs[i]/1.00 == (int)zs[i]) {
				elog(WARNING,"Please check solution for non-seasonal part, maybe you should rise term_maxtime or change the algorithm");
				break;
			}
		}



	// save phis and thetas

	//phis
	memcpy(model->super.phis, zs, model->super.p*sizeof(double));
	memcpy(&(model->super.phis[model->super.p]), &(zs[model->super.p]), model->super.sp*sizeof(double));

	//thetas
	memcpy(model->super.thetas, &zs[model->super.p+model->super.sp], model->super.q*sizeof(double));
	memcpy(&(model->super.thetas[model->super.q]), &zs[model->super.sp+model->super.p+model->super.q], model->super.sq*sizeof(double));

	//save constants
	if(model->super.includeConstant) {
		model->super.constant[0]=zs[model->super.sp+model->super.p+model->super.q+model->super.sq];
	}
	pfree(zs);
	pfree(ubns);
	pfree(lbns);

	pfree(model->data);
	/* TODO: release model -> does not work yet */



	MemoryContextSwitchTo(old);
}

//done
Datum
getNextarmodelValue(PG_FUNCTION_ARGS) {
	int 				num;
	ArimaModel			*model;
	double				forecast;

	num = PG_GETARG_INT32(1);
	model = &(((ARModel *)PG_GETARG_POINTER(0))->super);

	
	if(num == 1) {
		/* we start a new forecast cycle -> reset the state of previous forecasts */
		initArimaForecast(model);
	}

	forecast = get_next_forecast(model);

	PG_RETURN_FLOAT8(forecast);
}


void restoreArModelParameterForModelGraph(ArimaModel *model,Relation parameterRelation,Oid modelOid)
{
Datum			*values = NULL;
	int				numvalues = 0,maxC;
	ArrayType		**array;

	int				i;



	array = palloc(8*sizeof(*array));
RetrieveParametersMg(modelOid,array,5,parameterRelation);
	

	/* load element counts */
	deconstruct_array(array[0],INT4OID,sizeof(int4),true,'i',&values,NULL,&numvalues);

	model->includeConstant = DatumGetInt32(values[5]);
	model->p = DatumGetInt32(values[1]);
	model->d = DatumGetInt32(values[6]);
	model->q = DatumGetInt32(values[3]);
	model->sp=DatumGetInt32(values[2]);
	model->sd=DatumGetInt32(values[7]);
	model->sq=DatumGetInt32(values[4]);
	model->pd=DatumGetInt32(values[8]);
	maxC=Max(model->p,(model->sp*model->pd));


	pfree(values);

	if(model->p+model->sp){
	/* load AR coefficients */
	deconstruct_array(array[1],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->phis = palloc0((model->p+model->sp) * sizeof(double));
	for(i = 0; i<model->p+model->sp; i++)
		{
			model->phis[i] = DatumGetFloat8(values[i]);
		}
	pfree(values);
		/* load previous function values */
	deconstruct_array(array[2],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->y = palloc(maxC*sizeof(double));
	for(i = 0; i<maxC;i++)
		{
			model->y[i] = DatumGetFloat8(values[i]);
		}
	model->ycount =maxC;
	pfree(values);
	}
	
	if(model->q+model->sq+model->includeConstant){
	/* load MA coefficients and constant*/
	deconstruct_array(array[3],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->thetas = palloc((model->q+model->sq) * sizeof(*model->thetas));
	for(i = 0; i<model->q+model->sq; i++)
		model->thetas[i] = DatumGetFloat8(values[i]);
	if(model->includeConstant) {
		model->constant[1] = DatumGetFloat8(values[i]);
		i++;
		model->constant[0] = DatumGetFloat8(values[i]);
	}
	pfree(values);
	
	/* load stored error terms */
	deconstruct_array(array[4],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->uhat= palloc((Max(model->q,model->sq*model->pd))*sizeof(double)); 
	for(i=0; i<(Max(model->q,model->sq*model->pd)); i++)
		model->uhat[i] = DatumGetFloat8(values[i]);
	pfree(values);
	pfree(array);
	}

	
}

void backupArModelParameterForModelGraph(ArimaModel *model,Relation parameterRelation,Oid modelOid)
{
ArrayType		*array;
	ArrayType		*array2;
	

	Datum			*values;
	Datum  			*values2;
	int				i;
	int maxC=Max(model->p,(model->sp*model->pd));


	/* save the count for the elements */
	values = palloc(9*sizeof(*values));
	values[0] = Int32GetDatum(model->seasonType);
	values[1] = Int32GetDatum(model->p);
	values[2] = Int32GetDatum(model->sp);
	values[3] = Int32GetDatum(model->q);
	values[4] = Int32GetDatum(model->sq);
	values[5] = Int32GetDatum(model->includeConstant);
	values[6] = Int32GetDatum(model->d);
	values[7] = Int32GetDatum(model->sd);
	values[8] = Int32GetDatum(model->pd);
	array = construct_array(values,9,INT4OID,sizeof(int4),true,'i');
	InsertOrReplaceParametersMg(modelOid,parameterRelation,1,array);
	pfree(values);
//	pfree(array);



	/* save AR coefficients and last seen function values*/
	values = palloc((model->p+model->sp)*sizeof(*values));
	values2 = palloc(maxC*sizeof(*values2));
	for(i = 0; i<model->p+model->sp; i++) {
		values[i] = Float8GetDatum(model->phis[i]);
	}
	for(i=0; i<maxC; i++)
		if(model->y !=NULL)
			values2[i] = Float8GetDatum(model->y[i]);


	array = construct_array(values,model->p+model->sp,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	if(model->y !=NULL)
		array2 = construct_array(values2,maxC,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParametersMg(modelOid,parameterRelation,2,array);
	InsertOrReplaceParametersMg(modelOid,parameterRelation,3,array2);

	pfree(values);
	pfree(values2);
	pfree(array);
	pfree(array2);


	/* save MA coefficients + constant */
	values = palloc((model->q+model->sq + (2*model->includeConstant))*sizeof(double));
	for(i=0; i<model->q+model->sq; i++)
		values[i] = Float8GetDatum(model->thetas[i]);
	if(model->includeConstant) {
		values[model->q+model->sq] = Float8GetDatum(model->constant[1]);
		values[model->q+model->sq+1] = Float8GetDatum(model->constant[0]);
	}
	
	array = construct_array(values,(model->q+model->sq + (2*model->includeConstant)),FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParametersMg(modelOid,parameterRelation,4,array);
	pfree(values);
	pfree(array);


	/* save values for the MA Part */
	values = palloc(Max(model->q,(model->sq*model->pd))*sizeof(double));
	for(i=0; i<Max(model->q,(model->sq*model->pd)); i++)
		values[i] = Float8GetDatum(model->uhat[i]);
	array = construct_array(values,Max(model->q,(model->sq*model->pd)),FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParametersMg(modelOid,parameterRelation,5,array);
	pfree(values);
	pfree(array);	
	
}
//done
void storearModelParameters(PG_FUNCTION_ARGS) {
ArrayType		*array;
	ArrayType		*array2;
	ArimaModel		*model = &(((ARModel *)PG_GETARG_POINTER(0))->super);
	Oid				modelOid = PG_GETARG_OID(1);
	Datum			*values;
	Datum  			*values2;
	int				i;
	int maxC=Max(model->p,(model->sp*model->pd));


	/* save the count for the elements */
	values = palloc(9*sizeof(*values));
	values[0] = Int32GetDatum(model->seasonType);
	values[1] = Int32GetDatum(model->p);
	values[2] = Int32GetDatum(model->sp);
	values[3] = Int32GetDatum(model->q);
	values[4] = Int32GetDatum(model->sq);
	values[5] = Int32GetDatum(model->includeConstant);
	values[6] = Int32GetDatum(model->d);
	values[7] = Int32GetDatum(model->sd);
	values[8] = Int32GetDatum(model->pd);
	array = construct_array(values,9,INT4OID,sizeof(int4),true,'i');
	InsertOrReplaceParameters(modelOid,1,array);
	pfree(values);
//	pfree(array);



	/* save AR coefficients and last seen function values*/
	values = palloc((model->p+model->sp)*sizeof(*values));
	values2 = palloc(maxC*sizeof(*values2));
	for(i = 0; i<model->p+model->sp; i++) {
		values[i] = Float8GetDatum(model->phis[i]);
	}
	for(i=0; i<maxC; i++)
		if(model->y !=NULL)
			values2[i] = Float8GetDatum(model->y[i]);


	array = construct_array(values,model->p+model->sp,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	if(model->y !=NULL)
		array2 = construct_array(values2,maxC,FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParameters(modelOid,2,array);
	InsertOrReplaceParameters(modelOid,3,array2);

	pfree(values);
	pfree(values2);
	pfree(array);
	pfree(array2);


	/* save MA coefficients + constant */
	values = palloc((model->q+model->sq + (2*model->includeConstant))*sizeof(double));
	for(i=0; i<model->q+model->sq; i++)
		values[i] = Float8GetDatum(model->thetas[i]);
	if(model->includeConstant) {
		values[model->q+model->sq] = Float8GetDatum(model->constant[1]);
		values[model->q+model->sq+1] = Float8GetDatum(model->constant[0]);
	}
	
	array = construct_array(values,(model->q+model->sq + (2*model->includeConstant)),FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParameters(modelOid,4,array);
	pfree(values);
	pfree(array);


	/* save values for the MA Part */
	values = palloc(Max(model->q,(model->sq*model->pd))*sizeof(double));
	for(i=0; i<Max(model->q,(model->sq*model->pd)); i++)
		values[i] = Float8GetDatum(model->uhat[i]);
	array = construct_array(values,Max(model->q,(model->sq*model->pd)),FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d');
	InsertOrReplaceParameters(modelOid,5,array);
	pfree(values);
	pfree(array);	

}
//done
void loadarModelParameters(PG_FUNCTION_ARGS) {
	Datum			*values = NULL;
	int				numvalues = 0;
	ArimaModel		*model;
	ArrayType		**array;
	Oid				modelOid;
	int				i,maxC;

	model = &(((ARModel *)PG_GETARG_POINTER(0))->super);
	modelOid = PG_GETARG_OID(1);

	array = palloc(8*sizeof(*array));
	RetrieveParameters(modelOid,array,5);

	/* load element counts */
	deconstruct_array(array[0],INT4OID,sizeof(int4),true,'i',&values,NULL,&numvalues);

	model->includeConstant = DatumGetInt32(values[5]);
	model->p = DatumGetInt32(values[1]);
	model->d = DatumGetInt32(values[6]);
	model->q = DatumGetInt32(values[3]);
	model->sp=DatumGetInt32(values[2]);
	model->sd=DatumGetInt32(values[7]);
	model->sq=DatumGetInt32(values[4]);
	model->pd=DatumGetInt32(values[8]);
	maxC=Max(model->p,(model->sp*model->pd));


	pfree(values);

	if(model->p+model->sp){
	/* load AR coefficients */
	deconstruct_array(array[1],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->phis = palloc0((model->p+model->sp) * sizeof(double));
	for(i = 0; i<model->p+model->sp; i++)
		{
			model->phis[i] = DatumGetFloat8(values[i]);
		}
	pfree(values);
		/* load previous function values */
	deconstruct_array(array[2],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->y = palloc(maxC*sizeof(double));
	for(i = 0; i<maxC;i++)
		{
			model->y[i] = DatumGetFloat8(values[i]);
		}
	model->ycount =maxC;
	pfree(values);
	}
	
	if(model->q+model->sq+model->includeConstant){
	/* load MA coefficients and constant*/
	deconstruct_array(array[3],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->thetas = palloc((model->q+model->sq) * sizeof(*model->thetas));
	for(i = 0; i<model->q+model->sq; i++)
		model->thetas[i] = DatumGetFloat8(values[i]);
	if(model->includeConstant) {
		model->constant[1] = DatumGetFloat8(values[i]);
		i++;
		model->constant[0] = DatumGetFloat8(values[i]);
	}
	pfree(values);
	
	/* load stored error terms */
	deconstruct_array(array[4],FLOAT8OID,sizeof(float8),FLOAT8PASSBYVAL,'d',&values,NULL,&numvalues);
	model->uhat= palloc((Max(model->q,model->sq*model->pd))*sizeof(double)); 
	for(i=0; i<(Max(model->q,model->sq*model->pd)); i++)
		model->uhat[i] = DatumGetFloat8(values[i]);
	pfree(values);
	pfree(array);
	}



}

//done
void incrementalUpdatearModel(PG_FUNCTION_ARGS) {
	elog(WARNING,"not yet supported");
}

void incrementalUpdatearModel1(PG_FUNCTION_ARGS) {
	elog(WARNING,"not yet supported");
}
void incrementalUpdatearModel2(PG_FUNCTION_ARGS){
	elog(WARNING,"not yet supported");
}


void reestimateArModelParameters(PG_FUNCTION_ARGS)
{
	ARModel *m;
	MemoryContext 	old;
	DestReceiver 		*tupledest;
	Portal				portal;
	List *query_list,*planned_list,*parsetree_list,*helpList;
	ListCell *lc;
	ModelInfo *model=(ModelInfo*)PG_GETARG_POINTER(0);
	m=(ARModel*)model->model;

	
	old = MemoryContextSwitchTo(m->dataCtx);

		m->data = palloc(2*sizeof(double*));
	m->data[0] = palloc(512*sizeof(double));
	m->data[1] = palloc(512*sizeof(double));
	m->maxObs = 512;
	m->obsCount = 0;
	parsetree_list=pg_parse_query(model->trainingData);
	query_list = pg_analyze_and_rewrite((Node *)lfirst(list_head(parsetree_list)), model->trainingData, NULL, 0);
	planned_list = pg_plan_queries(query_list, 0, NULL);
	tupledest = CreateModelGraphDestReceiver();
	portal = CreateNewPortal();
	//Don't display the portal in pg_cursors, it is for internal use only
	portal->visible = false;
	PortalDefineQuery(portal, NULL, model->trainingData, "SELECT", planned_list, NULL);
	PortalStart(portal, NULL, InvalidSnapshot);
	(void) PortalRun(portal, FETCH_ALL, false, tupledest, tupledest, NULL);
	
	helpList = ((ModelGraphState *)tupledest)->tupleList;
	foreach(lc, helpList)
	{
		processForecastModel(model, ((Datum*)lfirst(lc))[model->measure->resno-1]);

	}
	MemoryContextSwitchTo(old);

	finalizeForecastModel(model);

	(*tupledest->rDestroy) (tupledest);
	PortalDrop(portal, false);
	
	//if table, update parameters
	if(model->storeModel==3 || model->storeModel==13)
	{
		storeModelParameters(model, model->modelOid);
	}
	
	
}