OUTPUT=colorfill.jar
TMPDIR=_tmp_
TMPLIBZIP=lib/_tmplib.zip

if [ ! -f $TMPLIBZIP ]; then
    rm -rf $TMPDIR
    mkdir $TMPDIR
    unzip -q lib/designgridlayout-1.11.jar -x "META-INF/*" -d $TMPDIR
    unzip -q lib/flatlaf-0.45.jar          -x "META-INF/*" -d $TMPDIR
    cd $TMPDIR
    advzip -a -4 -i 1000 ../$TMPLIBZIP *
    cd ..
fi

rm -f $OUTPUT _$OUTPUT $OUTPUT.zip
java -jar $HOME/Programme/proguard6.2/lib/proguard.jar @proguard_config -outjars _$OUTPUT

rm -rf $TMPDIR
mkdir $TMPDIR
unzip -q _$OUTPUT -d $TMPDIR
cd $TMPDIR
#advzip -a -q -3 ../$OUTPUT.zip *
advzip -a -4 -i 1000 ../$OUTPUT.zip *
cd ..
zipmerge $OUTPUT $OUTPUT.zip $TMPLIBZIP
rm -rf $TMPDIR _$OUTPUT $OUTPUT.zip

chmod u+x $OUTPUT
