CREATE OR REPLACE FUNCTION etsForecast(vals float8[], num integer) RETURNS float8[] AS '
    library(forecast)
	
	prog <- forecast(ets(vals),h=num)
	return(prog$mean)
' LANGUAGE 'plr' STRICT;