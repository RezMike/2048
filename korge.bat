@ECHO OFF

SETLOCAL EnableDelayedExpansion

SET INSTALLER_VERSION=0.1.1
SET INSTALLER_URL=https://github.com/korlibs/compiler.korge.org/releases/download/v%INSTALLER_VERSION%/korge-kotlin-compiler-all.tar.xz
SET INSTALLER_SHA256=c34605db97a7bab75c661d3df3faec6961410a5159083d3b9da347cda0c9d3ce

SET KORGEDIR=%USERPROFILE%\.korge
SET JAVA=%KORGEDIR%\jre-21\bin\java.exe
SET INSTALLER_PATH=%KORGEDIR%\compiler
SET INSTALLER_LOCAL_FILE=%INSTALLER_PATH%\korge-kotlin-compiler-all.%INSTALLER_VERSION%.jar
MKDIR "%INSTALLER_PATH%" 2> NUL

IF NOT EXIST "%INSTALLER_LOCAL_FILE%" (
    CALL :DOWNLOAD_FILE "%INSTALLER_URL%" "%INSTALLER_LOCAL_FILE%.tar.xz" "%INSTALLER_SHA256%"
    CALL :EXTRACT_TAR "%INSTALLER_LOCAL_FILE%.tar.xz" "%INSTALLER_PATH%" 0 "%INSTALLER_LOCAL_FILE%"
    COPY /Y "%INSTALLER_PATH%\korge-kotlin-compiler-all.jar" %INSTALLER_LOCAL_FILE% > NUL 2> NUL
    DEL "%INSTALLER_PATH%\korge-kotlin-compiler-all.jar" > NUL 2> NUL
    DEL "%INSTALLER_LOCAL_FILE%.tar.xz" > NUL 2> NUL
    DEL "%INSTALLER_LOCAL_FILE%.tar.xz.sha256" > NUL 2> NUL
)

IF NOT EXIST "%JAVA%" (
    IF "%PROCESSOR_ARCHITECTURE%" == "ARM64" (
        CALL :DOWNLOAD_FILE "https://github.com/korlibs/universal-jre/releases/download/0.0.1/microsoft-jre-21.0.3-windows-aarch64.tar.xz" "%KORGEDIR%\jre-21.tar.xz" "8F18060960FD7935D76C79BBB643B1779440B73AE9715153A3BA332B1B8A2348"
    ) ELSE (
        CALL :DOWNLOAD_FILE "https://github.com/korlibs/universal-jre/releases/download/0.0.1/microsoft-jre-21.0.3-windows-x64.tar.xz" "%KORGEDIR%\jre-21.tar.xz" "6D16528A2201DCBE0ADDB0622F5CBE0CD6FA84AE937D3830FC1F74B32132C37B"
    )
    CALL :EXTRACT_TAR "%KORGEDIR%\jre-21.tar.xz" "%KORGEDIR%\jre-21" 1 "%JAVA%"
    DEL "%KORGEDIR%\jre-21.tar.xz" > NUL 2> NUL
    DEL "%KORGEDIR%\jre-21.tar.xz.sha256" > NUL 2> NUL
)

"%JAVA%" -jar "%INSTALLER_LOCAL_FILE%" %* & SET ERRORLEVEL=!ERRORLEVEL! & CALL;

EXIT /B %ERRORLEVEL%

:DOWNLOAD_FILE
    SET URL=%~1
    SET LOCAL_PATH=%~2
    SET EXPECTED_SHA256=%~3

    IF EXIST "%LOCAL_PATH%" (
        EXIT /B
    )

    IF NOT EXIST "%LOCAL_PATH%.tmp" (
        echo Downloading %URL% into %LOCAL_PATH%
        curl -sL "%URL%" -o "%LOCAL_PATH%.tmp"
    )
    powershell -NoProfile -ExecutionPolicy Bypass -Command "(Get-Filehash -Path '%LOCAL_PATH:\=\\%.tmp' -Algorithm SHA256).Hash" > "%LOCAL_PATH%.sha256"
    SET /p DOWNLOAD_SHA256=<"%LOCAL_PATH%.sha256"

    IF /i "%DOWNLOAD_SHA256%" == "%EXPECTED_SHA256%" (
        MOVE "%LOCAL_PATH%.tmp" "%LOCAL_PATH%" > NUL 2> NUL
    ) ELSE (
        ECHO ERROR downloading %URL%, SHA256=%DOWNLOAD_SHA256%, but expected SHA256=%EXPECTED_SHA256%
        EXIT /B -1
    )
EXIT /b

:EXTRACT_TAR
    SET INPUT_FILE=%~1
    SET OUT=%~2
    SET STRIP_COMPONENTS=%~3
    SET CHECK_EXISTS=%~4

    IF EXIST %CHECK_EXISTS% (
        EXIT /B
    )

    MKDIR "%OUT%" > NUL 2> NUL
    echo Extracting %INPUT_FILE%...
    tar --strip-components %STRIP_COMPONENTS% -C "%OUT%" -xf "%INPUT_FILE%"
EXIT /b

:NORMALIZE_PATH
  SET RETVAL=%~f1
EXIT /B