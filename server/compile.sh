set -e
rm -fr bin
path_dir=${PWD##*/}
rm -f "${path_dir}.jar"
mkdir bin
cd bin
FILES="../lib/*"
for f in $FILES
do
  echo "Compiling $f"
    jar xf $f
done
rm -fr META-INF/*.SF
rm -fr META-INF/*.RSA
rm -fr META-INF/*.DSA
rm -fr META-INF/MANIFEST.MF
# module-info.class de los jars extraidos: si queda, javac trata bin/ como un modulo
# y falla resolviendo "requires" que no estan en lib/ (ej: jakarta.websocket.client)
rm -f module-info.class
echo "Manifest-Version: 1.0" >> META-INF/MANIFEST.MF
echo "Main-Class: App" >> META-INF/MANIFEST.MF
echo "" >> META-INF/MANIFEST.MF
echo "" >> META-INF/MANIFEST.MF
cd ../
javac -cp "src/:lib/*" src/App.java -d bin
jar cfM "${path_dir}.jar" -C bin/ .
rm -fr bin
#Copy to USUARIO
#cp "${path_dir}.jar" /Users/rickypazd/Documents/GitHub/usuario/server/lib/
#cp "${path_dir}.jar" /Users/rickypazd/Documents/GitHub/kolping/server/lib/