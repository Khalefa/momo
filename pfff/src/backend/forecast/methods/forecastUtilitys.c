#include "forecast/methods/forecastUtilitys.h"


double
calculateOneStepSMAPE(double value, double forecast) {
	return (double) abs(value - forecast) / ((value + forecast) / 2.0);
}