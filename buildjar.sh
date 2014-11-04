OUTPUT=colorfill.jar

rm -f $OUTPUT

java -jar $HOME/Programme/proguard5.1/lib/proguard.jar @proguard_config -outjars $OUTPUT

chmod u+x $OUTPUT
