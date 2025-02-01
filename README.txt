ColorFill - yet another Flood-It clone (game and solver algorithm)

Version   1.3.3 (2023-08-25)
Homepage  https://github.com/smack42/ColorFill/wiki



about

The game called Flood-It has been around for some years. There are many
clones and variants available for desktop and mobile platforms.

The game board is a grid of squares, colored at random in multiple colors.
In each move the player changes the color of the "start square" and all squares
of the same color that are connected to it. The objective is to fill the entire
grid in a single color using as few moves as possible.

This program, ColorFill, is yet another clone of this game. It includes an
interactive GUI mode which lets you play the puzzles and explore the solutions
that its integrated solver algorithms have found.

The program also has a dedicated "solver mode" that uses the algorithms to
solve two competition tasks that I've found on the internet:
- "Programming Challenge 19" (1000 14x14 boards; see directory pc19)
- "Code Golf 26232" (100000 19x19 boards; see directory codegolf26232)



usage

Java SE Runtime Environment (JRE version 8 or newer) is required to run
this program. You can download Java here:
https://www.oracle.com/java/technologies/downloads/
https://adoptium.net/

To run the program just doubleclick "colorfill.jar".

On the command line run it like this:
  java -jar colorfill.jar



license

ColorFill game and solver
Copyright (C) 2023 Michael Henke <smack42@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.



This program uses:

DesignGridLayout - Swing LayoutManager that implements "Canonical Grids"
- https://web.archive.org/web/20170409233103/https://java.net/projects/designgridlayout/pages/Home
- https://search.maven.org/artifact/net.java.dev.designgridlayout/designgridlayout
- Copyright 2005-2013 Jason Aaron Osgood, Jean-Francois Poilpret
- DesignGridLayout is open source licensed under the Apache License 2.0

FlatLaf - Flat Look and Feel (with Darcula/IntelliJ themes support)
- https://www.formdev.com/flatlaf
- https://github.com/JFormDesigner/FlatLaf
- Copyright 2019 FormDev Software GmbH
- FlatLaf is open source licensed under the Apache License 2.0




some links

Online game
    http://unixpapa.com/floodit/

Android app
    https://play.google.com/store/apps/details?id=name.boyle.chris.sgtpuzzles
    https://play.google.com/store/apps/details?id=com.labpixies.flood
    https://play.google.com/store/apps/details?id=com.wetpalm.colorflood

Programming
    https://web.archive.org/web/20150909200653/http://cplus.about.com/od/programmingchallenges/a/challenge19.htm
    https://stackoverflow.com/questions/1430962/how-to-optimally-solve-the-flood-fill-puzzle
    http://markgritter.livejournal.com/tag/floodit
    http://kunigami.wordpress.com/2012/09/16/flood-it-an-exact-approach/
    https://codegolf.stackexchange.com/questions/26232/create-a-flood-paint-ai
    https://github.com/aaronpuchert/floodit
    https://github.com/Flolle/terminal-flood
    https://www.formdev.com/flatlaf   https://github.com/JFormDesigner/FlatLaf
    https://github.com/manteuffel723/flood-it-boards    https://doc.neuro.tu-berlin.de/bachelor/2023-BA-PhilippVonManteuffel-mc.pdf
