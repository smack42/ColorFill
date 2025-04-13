OUTPUT=colorfill.jar

jar --create --file $OUTPUT --no-compress --manifest src/META-INF/MANIFEST.MF -C bin/ colorfill
chmod u+x $OUTPUT
