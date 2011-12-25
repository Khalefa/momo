@echo off

REM Specify appropriate SWT library!

set swtjar=libs/ext/swt/swt-win32-win32-x86_64.jar

cmd /V /C "@echo off&&SET jars=%swtjar%&&(for %%i in (libs/*.jar) do (SET jars=!jars!;libs/%%i))&&java -Xmx512M -classpath build/classes;!jars! -Djava.library.path=%swtjar%; Demo"
