package fr.xyness.SCS;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * Class to manage scoreboard
 */
public class CScoreboard {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/* The scoreboard instance */
    private Scoreboard scoreboard;
    
    /* The objective instance */
    private Objective objective;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Initializes a new scoreboard and creates an objective for it.
     *
     * @param title The title of the scoreboard.
     */
    public CScoreboard(String title) {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        objective = scoreboard.registerNewObjective(" ", " ", title);
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Adds a line to the scoreboard with a specific score.
     *
     * @param line  The text to display on the scoreboard line.
     * @param score The score to display next to the line.
     */
    public void addLine(String line, int score) {
        Score s = objective.getScore(line);
        s.setScore(score);
    }

    /**
     * Sets the scoreboard to a specific player, showing it in their sidebar.
     *
     * @param player The player to show the scoreboard to.
     */
    public void showToPlayer(Player player) {
    	player.setScoreboard(scoreboard);
    }

    /**
     * Removes the scoreboard from the player (restores the default scoreboard).
     *
     * @param player The player to remove the scoreboard from.
     */
    public void removeFromPlayer(Player player) {
    	player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * Updates a specific line on the scoreboard with a new score.
     * Removes the old line and adds a new one with the updated score.
     *
     * @param oldLine The old line to replace.
     * @param newLine The new line to set.
     * @param score   The new score for the updated line.
     */
    public void updateLine(String oldLine, String newLine, int score) {
        scoreboard.resetScores(oldLine);
        addLine(newLine, score);
    }
    
    /**
     * Updates specific lines on the scoreboard based on a map of scores and lines.
     * Removes the old line associated with each score and adds the new line from the map.
     *
     * @param linesMap The map where keys are scores and values are the new lines to set.
     */
    public void updateLines(Map<Integer, String> linesMap) {
        for (Map.Entry<Integer, String> entry : linesMap.entrySet()) {
            int score = entry.getKey();
            String newLine = entry.getValue();
            for (String existingEntry : scoreboard.getEntries()) {
                if (objective.getScore(existingEntry).getScore() == score) {
                    scoreboard.resetScores(existingEntry);
                    break;
                }
            }
            addLine(newLine, score);
        }
    }

    /**
     * Clears all lines from the scoreboard.
     */
    public void clear() {
    	if(scoreboard != null) {
    		scoreboard.getEntries().forEach(scoreboard::resetScores);
    	}
    }
	
}
