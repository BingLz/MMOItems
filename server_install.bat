@echo off

rem ==========================================================
rem Windows script to build the plugin using Maven
rem and automatically install the built JAR into the server
rem ==========================================================

set "source_file=target\*.jar"

rem Remove all existing jars in the target folder
rem Needed for branches with different final JAR names built on the same computer
del /Q "%source_file%"

rem Custom Maven build command
call mvn clean install

rem Loop over all provided arguments (each is a folder path)
:copy_loop
if "%~1"=="" goto :eof

set "folder=%~1"
echo Copying to "%folder%\plugins"
copy "%source_file%" "%folder%\plugins" /Y

shift
goto copy_loop
