ColorFill - yet another Flood-It clone (game and solver algorithm)

Version   1.1.2 (2018-10-02)
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

Java SE Runtime Environment (JRE version 6 or newer) is required to run
this program. You can download Java here:
http://www.oracle.com/technetwork/java/javase/downloads/index.html

To run the program just doubleclick "colorfill.jar".

On the command line run it like this:
  java -jar colorfill.jar



license

ColorFill game and solver
Copyright (C) 2018 Michael Henke <smack42@gmail.com>

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
