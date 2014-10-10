/*  ColorFill game and solver
    Copyright (C) 2014 Michael Henke

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
*/

package colorfill;

public class Starter {

    public static void main(String[] args) {
        final String b = "1162252133131612635256521232523162563651114141545542546462521536446531565521654652142612462122432145511115534353355111125242362245623255453446513311451665625534126316211645151264236333165263163254";
        final String s = "6345215456513263145";
        final Board board = new Board(b);
        final String result = board.checkSolution(s, 0);
        System.out.println(board);
        System.out.println(s + "_" + s.length());
        if (result.isEmpty()) {
            System.out.println("solution check OK");
        } else {
            System.out.println(result);
        }
    }
}
