mkdir build 2>/dev/null
cd build
[ -e plugin.yml ] || ln -s ../plugin.yml
[ -e config.yml ] || ln -s ../config.yml
[ -e version.txt ] || ln -s ../version.txt
[ -d com ] || ln -s ../com
# make sure bukkit is in CLASSPATH!
find com -type f -exec javac {} +
jar cf ../RedstoneSensor.jar .
