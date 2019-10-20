@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  PolicyMig startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Add default JVM options here. You can also use JAVA_OPTS and POLICY_MIG_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto init

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto init

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:init
@rem Get command-line arguments, handling Windows variants

if not "%OS%" == "Windows_NT" goto win9xME_args

:win9xME_args
@rem Slurp the command line arguments.
set CMD_LINE_ARGS=
set _SKIP=2

:win9xME_args_slurp
if "x%~1" == "x" goto execute

set CMD_LINE_ARGS=%*

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\PolicyMig-1.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.3.50.jar;%APP_HOME%\lib\clikt-2.2.0.jar;%APP_HOME%\lib\logback-classic-1.2.3.jar;%APP_HOME%\lib\exposed-0.17.4.jar;%APP_HOME%\lib\ec2-2.5.29.jar;%APP_HOME%\lib\aws-query-protocol-2.5.29.jar;%APP_HOME%\lib\protocol-core-2.5.29.jar;%APP_HOME%\lib\aws-core-2.5.29.jar;%APP_HOME%\lib\auth-2.5.29.jar;%APP_HOME%\lib\regions-2.5.29.jar;%APP_HOME%\lib\sdk-core-2.5.29.jar;%APP_HOME%\lib\apache-client-2.5.29.jar;%APP_HOME%\lib\netty-nio-client-2.5.29.jar;%APP_HOME%\lib\http-client-spi-2.5.29.jar;%APP_HOME%\lib\profiles-2.5.29.jar;%APP_HOME%\lib\utils-2.5.29.jar;%APP_HOME%\lib\slf4j-api-1.7.28.jar;%APP_HOME%\lib\logback-core-1.2.3.jar;%APP_HOME%\lib\mysql-connector-java-8.0.17.jar;%APP_HOME%\lib\google-api-services-compute-v1-rev214-1.25.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.3.50.jar;%APP_HOME%\lib\kotlin-reflect-1.3.50.jar;%APP_HOME%\lib\kotlinx-coroutines-core-1.3.0-M1.jar;%APP_HOME%\lib\kotlin-stdlib-1.3.50.jar;%APP_HOME%\lib\joda-time-2.10.2.jar;%APP_HOME%\lib\h2-1.4.199.jar;%APP_HOME%\lib\protobuf-java-3.6.1.jar;%APP_HOME%\lib\google-api-client-1.25.0.jar;%APP_HOME%\lib\annotations-2.5.29.jar;%APP_HOME%\lib\kotlin-stdlib-common-1.3.50.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\google-oauth-client-1.25.0.jar;%APP_HOME%\lib\google-http-client-jackson2-1.25.0.jar;%APP_HOME%\lib\guava-20.0.jar;%APP_HOME%\lib\google-http-client-1.25.0.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\jackson-databind-2.9.8.jar;%APP_HOME%\lib\jackson-core-2.9.8.jar;%APP_HOME%\lib\netty-reactive-streams-http-2.0.0.jar;%APP_HOME%\lib\netty-reactive-streams-2.0.0.jar;%APP_HOME%\lib\reactive-streams-1.0.2.jar;%APP_HOME%\lib\flow-1.7.jar;%APP_HOME%\lib\jackson-annotations-2.9.0.jar;%APP_HOME%\lib\httpclient-4.5.6.jar;%APP_HOME%\lib\httpcore-4.4.10.jar;%APP_HOME%\lib\netty-codec-http2-4.1.33.Final.jar;%APP_HOME%\lib\netty-codec-http-4.1.33.Final.jar;%APP_HOME%\lib\netty-handler-4.1.33.Final.jar;%APP_HOME%\lib\netty-codec-4.1.33.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.1.33.Final-linux-x86_64.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.33.Final.jar;%APP_HOME%\lib\netty-transport-4.1.33.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.33.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.33.Final.jar;%APP_HOME%\lib\netty-common-4.1.33.Final.jar;%APP_HOME%\lib\j2objc-annotations-1.1.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\commons-codec-1.10.jar

@rem Execute PolicyMig
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %POLICY_MIG_OPTS%  -classpath "%CLASSPATH%" policymig.PolicyMig %CMD_LINE_ARGS%

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable POLICY_MIG_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%POLICY_MIG_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
