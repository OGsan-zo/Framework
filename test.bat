@echo off
rem Compilation
javac -proc:full -parameters --release 17 -d ".\bin" -cp ".\lib\*" Compile\*.java

REM Copier les classes compilées vers le répertoire Test (pour les tests)
xcopy /E /I /Y bin\controller "..\Test\WEB-INF\classes\controller"

REM Se déplacer dans le répertoire bin pour exécuter le programme
cd bin

REM Exécution du programme Java en incluant le fichier JAR dans le classpath
java -cp ".;..\lib\*" other.Main
@REM java -cp ".;..\lib\*" model.ExempleSansHashCode

REM Revenir au répertoire précédent
cd ..
