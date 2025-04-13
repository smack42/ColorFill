DESTDIR=ColorFill_release

rm -rf $DESTDIR
mkdir $DESTDIR

jar --create --file $DESTDIR/colorfill.jar --no-compress --manifest src/META-INF/MANIFEST.MF -C bin/ colorfill
chmod u+x $DESTDIR/colorfill.jar

mkdir $DESTDIR/lib
cp -p  src/*.properties               $DESTDIR/lib
cp -p  src/*.png                      $DESTDIR/lib
cp -p  lib/designgridlayout-1.11.jar  $DESTDIR/lib
cp -p  lib/flatlaf-*-no-natives.jar   $DESTDIR/lib
cp -p  lib/flatlaf-*.so               $DESTDIR/lib
cp -p  lib/flatlaf-*.dylib            $DESTDIR/lib
cp -p  lib/flatlaf-*.dll              $DESTDIR/lib

cp -p  CHANGES.txt                    $DESTDIR
cp -p  LICENSE.txt                    $DESTDIR
cp -p  README.txt                     $DESTDIR

cp -rp src                            $DESTDIR
