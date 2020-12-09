/*  ColorFill game and solver
    Copyright (C) 2014, 2015, 2016 Michael Henke

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

package colorfill.ui;

import java.util.ResourceBundle;

import javax.swing.SwingUtilities;

import colorfill.model.GameState;
import colorfill.model.StartPositionEnum;

public class GameIdController {

    private static final ResourceBundle L10N = ResourceBundle.getBundle("colorfill-ui");  //L10N = Localization

    private final MainController mainController;
    private final GameState gameState;
    private final GameIdDialog dialog;

    protected GameIdController(final MainController mainController, final GameState gameState, final MainWindow mainWindow) {
        this.mainController = mainController;
        this.gameState = gameState;
        this.dialog = new GameIdDialog(this, mainWindow);
    }

    protected void showDialog() {
        this.dialog.showDialog();
    }

    protected String getCurrentGameId() {
        return this.gameState.getGameId();
    }

    protected String getInfoGameId(final String gameId) {
        try {
            final GameState gs = GameState.tryInfoGameId(gameId);
            final StartPositionEnum spe = StartPositionEnum.valueOf(StartPositionEnum.intValueFromPosition(gs.getBoard().getStartPos(), gs.getBoard().getWidth(), gs.getBoard().getHeight()));
            final String startPos = (null != spe ? L10N.getString(spe.l10nKey) : Integer.toString(gs.getBoard().getStartPos()));
            final StringBuilder sb = new StringBuilder();
            sb  .append(L10N.getString("gameId.txt.Size.txt")).append(" ")
                .append(gs.getBoard().getWidth())
                .append("x")
                .append(gs.getBoard().getHeight())
                .append(", ")
                .append(L10N.getString("gameId.txt.Colors.txt")).append(" ")
                .append(gs.getBoard().getNumColors())
                .append(", ")
                .append(L10N.getString("gameId.txt.StartPos.txt")).append(" ")
                .append(startPos)
                .append(", ")
                .append(L10N.getString("gameId.txt.Solution.txt")).append(" ")
                .append(gs.getSelectedProgress().getTotalSteps()).append(" ")
                .append(gs.getSelectedProgress().isFinished() ? L10N.getString("gameId.txt.complete.txt") : "")
                ;
            return sb.toString();
        } catch (final Exception e) {
            return "??? (" + e.getMessage() + ")";
        }
    }

    protected String applyGameId(final String gameId) {
        if (this.gameState.isSameGameId(gameId)) {
            return null;  // unchanged game ID - do nothing
        }
        final String result = this.gameState.applyGameId(gameId);
        if ((null != result) && (result.length() != 0)) {
            return result;  // error message
        } else {
            this.mainController.actionUpdatedPrefs(false, true, false);
            return null;
        }
    }
}
