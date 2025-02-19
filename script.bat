@echo off

set destination="..\Test\WEB-INF\lib\"
set bin=".\bin"
set "JAR_FILE=Framework"

REM Copier les fichiers Java du répertoire src\controller vers Test_Compile
xcopy /E /I /Y src\other\*.java "Compile"
xcopy /E /I /Y src\annotation\*.java "Compile"
xcopy /E /I /Y src\annotation\auth\*.java "Compile"
xcopy /E /I /Y src\annotation\methods\*.java "Compile"
xcopy /E /I /Y src\annotation\field\*.java "Compile"
xcopy /E /I /Y src\auth\*.java "Compile"
xcopy /E /I /Y src\exception\*.java "Compile"
xcopy /E /I /Y src\controller\*.java "Compile"
xcopy /E /I /Y src\servlet\*.java "Compile"
xcopy /E /I /Y ..\Test\src\controller\*.java "Compile"
xcopy /E /I /Y ..\Test\src\model\*.java "Compile"

REM Compiler les fichiers Java dans le répertoire de sortie
javac -proc:full -parameters --release 17 -d "%bin%" -cp ".\lib\*" Compile\*.java

REM Créer un fichier JAR contenant les classes compilées 
jar cvf ".\%JAR_FILE%.jar" -C "%bin%" .

REM copie du jar vers la destination 
xcopy /Y ".\%JAR_FILE%.jar" "%destination%"
