OUTPUT=colorfill.jar
TMPDIR=_tmp_

rm -f $OUTPUT _$OUTPUT

java -jar $HOME/Programme/proguard6.2/lib/proguard.jar @proguard_config -outjars _$OUTPUT

rm -rf $TMPDIR
mkdir $TMPDIR
unzip -q _$OUTPUT -d $TMPDIR

cd $TMPDIR
advzip -a -4 -i 256 ../$OUTPUT *
cd ..
rm -rf $TMPDIR

chmod u+x $OUTPUT
