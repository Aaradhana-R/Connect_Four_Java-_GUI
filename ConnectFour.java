import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Connect Four — Single-file Java Swing implementation
 *
 * Features:
 * - 7x6 classic grid
 * - Local 2-player mode or vs Computer (basic AI that blocks/wins if possible)
 * - Undo last move, Restart game
 * - Animated disc drop, win/draw detection, status bar
 * - Light/Dark themes toggle
 *
 * How to run:
 *   javac ConnectFour.java
 *   java ConnectFour
 */
public class ConnectFour extends JFrame {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConnectFour().setVisible(true));
    }

    // Game constants
    private static final int ROWS = 6;
    private static final int COLS = 7;

    // Model
    private final int[][] board = new int[ROWS][COLS]; // 0 empty, 1 P1 (Red), 2 P2 (Yellow)
    private int currentPlayer = 1;
    private boolean gameOver = false;
    private boolean vsComputer = false; // toggle via menu
    private int computerPlayer = 2; // which side the AI controls when vsComputer = true

    // Move history for Undo (store row and col as a small struct)
    private static class Move { int r, c; Move(int r, int c){this.r=r; this.c=c;} }
    private final Deque<Move> history = new ArrayDeque<>();

    // UI
    private final BoardPanel boardPanel = new BoardPanel();
    private final JLabel status = new JLabel("Red's turn (Player 1)");
    private boolean darkTheme = false;

    public ConnectFour(){
        super("Connect Four — Java Swing");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Menu
        setJMenuBar(createMenuBar());

        // Status bar
        status.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(status, BorderLayout.SOUTH);

        // Board
        add(boardPanel, BorderLayout.CENTER);

        // New game
        resetGame();
    }

    private JMenuBar createMenuBar(){
        JMenuBar mb = new JMenuBar();

        JMenu game = new JMenu("Game");
        JMenuItem newGame = new JMenuItem("New Game");
        newGame.addActionListener(e -> resetGame());
        JMenuItem undo = new JMenuItem("Undo");
        undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        undo.addActionListener(e -> undoMove());
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> System.exit(0));
        game.add(newGame);
        game.add(undo);
        game.addSeparator();
        game.add(exit);

        JMenu mode = new JMenu("Mode");
        JCheckBoxMenuItem vsAI = new JCheckBoxMenuItem("Play vs Computer");
        vsAI.addActionListener(e -> {
            vsComputer = vsAI.isSelected();
            // If switching on mid-game and it's AI's turn, let AI move
            maybeTriggerAIMove();
            updateStatus();
        });
        JMenuItem switchSides = new JMenuItem("Switch AI Side");
        switchSides.addActionListener(e -> {
            computerPlayer = (computerPlayer == 1) ? 2 : 1;
            updateStatus();
            maybeTriggerAIMove();
        });
        mode.add(vsAI);
        mode.add(switchSides);

        JMenu view = new JMenu("View");
        JCheckBoxMenuItem dark = new JCheckBoxMenuItem("Dark Theme");
        dark.addActionListener(e -> { darkTheme = dark.isSelected(); boardPanel.repaint(); });
        view.add(dark);

        mb.add(game);
        mb.add(mode);
        mb.add(view);
        return mb;
    }

    private void resetGame(){
        for(int r=0;r<ROWS;r++) Arrays.fill(board[r], 0);
        history.clear();
        currentPlayer = 1;
        gameOver = false;
        updateStatus();
        boardPanel.resetAnimation();
        boardPanel.repaint();
        maybeTriggerAIMove();
    }

    private void updateStatus(){
        if(gameOver){
            int winner = getWinner();
            if(winner==0) status.setText("It's a draw! Click Game > New Game to play again.");
            else status.setText((winner==1?"Red":"Yellow") + " wins! Click Game > New Game to play again.");
            return;
        }
        String turn = (currentPlayer==1?"Red":"Yellow");
        if(vsComputer){
            String who = (computerPlayer==currentPlayer?" (Computer)":" (You)");
            status.setText(turn + "'s turn" + who);
        } else {
            status.setText(turn + "'s turn (Player " + currentPlayer + ")");
        }
    }

    private void undoMove(){
        if(history.isEmpty() || gameOver) return;
        // If vs AI and last two moves were human+AI, undo both for convenience
        Move last = history.pollLast();
        board[last.r][last.c] = 0;
        currentPlayer = 3 - currentPlayer;
        if(vsComputer && !history.isEmpty()){
            // Undo AI move too so it's the human's turn again
            Move last2 = history.peekLast();
            if(last2 != null) {
                board[last2.r][last2.c] = 0;
                history.pollLast();
                currentPlayer = 3 - currentPlayer;
            }
        }
        gameOver = false;
        boardPanel.resetAnimation();
        boardPanel.repaint();
        updateStatus();
    }

    private void maybeTriggerAIMove(){
        if(gameOver) return;
        if(vsComputer && currentPlayer == computerPlayer){
            // Delay slightly for UX
            new javax.swing.Timer(350, e -> {
                ((javax.swing.Timer)e.getSource()).stop();
                int col = chooseAIMove();
                if(col >= 0) makeMove(col);
            }).start();
        }
    }

    private void makeMove(int col){
        if(gameOver) return;
        int row = findAvailableRow(col);
        if(row < 0) return; // column full
        animateDrop(row, col, currentPlayer);
    }

    private int findAvailableRow(int col){
        for(int r=ROWS-1; r>=0; r--){
            if(board[r][col]==0) return r;
        }
        return -1;
    }

    private void commitMove(int row, int col, int player){
        board[row][col] = player;
        history.addLast(new Move(row, col));
        // Check for win/draw
        if(checkWin(row, col, player)){
            gameOver = true;
        } else if(isBoardFull()){
            gameOver = true; // draw
        } else {
            currentPlayer = 3 - currentPlayer; // switch
        }
        updateStatus();
        boardPanel.repaint();
        if(!gameOver) maybeTriggerAIMove();
    }

    private boolean isBoardFull(){
        for(int c=0;c<COLS;c++) if(board[0][c]==0) return false;
        return true;
    }

    private int getWinner(){
        if(history.isEmpty()) return 0;
        Move last = history.peekLast();
        int p = board[last.r][last.c];
        if(p!=0 && checkWin(last.r, last.c, p)) return p;
        return 0;
    }

    private boolean checkWin(int row, int col, int player){
        int[][] dirs = {{0,1},{1,0},{1,1},{1,-1}};
        for(int[] d : dirs){
            int count = 1;
            count += countDir(row, col, d[0], d[1], player);
            count += countDir(row, col, -d[0], -d[1], player);
            if(count >= 4) return true;
        }
        return false;
    }

    private int countDir(int r, int c, int dr, int dc, int player){
        int cnt = 0;
        int rr = r + dr, cc = c + dc;
        while(rr>=0 && rr<ROWS && cc>=0 && cc<COLS && board[rr][cc]==player){
            cnt++; rr+=dr; cc+=dc;
        }
        return cnt;
    }

    private int chooseAIMove(){
        int me = computerPlayer;
        int opp = 3 - me;

        for(int c=0;c<COLS;c++){
            int r = findAvailableRow(c);
            if(r>=0){
                board[r][c] = me;
                boolean win = checkWin(r, c, me);
                board[r][c] = 0;
                if(win) return c;
            }
        }
        for(int c=0;c<COLS;c++){
            int r = findAvailableRow(c);
            if(r>=0){
                board[r][c] = opp;
                boolean win = checkWin(r, c, opp);
                board[r][c] = 0;
                if(win) return c;
            }
        }
        int[] pref = {3,2,4,1,5,0,6};
        for(int c : pref){
            if(findAvailableRow(c) >= 0) return c;
        }
        for(int c=0;c<COLS;c++) if(findAvailableRow(c)>=0) return c;
        return -1;
    }

    private void animateDrop(int targetRow, int col, int player){
        final int[] animRow = { -1 };
        javax.swing.Timer t = new javax.swing.Timer(12, null);
        t.addActionListener(e -> {
            if(animRow[0] < targetRow){
                animRow[0]++;
                boardPanel.setGhostDisc(animRow[0], col, player);
                boardPanel.repaint();
            } else {
                ((javax.swing.Timer)e.getSource()).stop();
                boardPanel.clearGhost();
                commitMove(targetRow, col, player);
            }
        });
        t.start();
    }

    private class BoardPanel extends JPanel {
        private final int padding = 30;
        private int ghostR = -1, ghostC = -1, ghostPlayer = 0;

        BoardPanel(){
            setBackground(new Color(240,240,240));
            setPreferredSize(new Dimension(820, 700));

            addMouseListener(new MouseAdapter(){
                @Override public void mousePressed(MouseEvent e){
                    if(gameOver) return;
                    if(vsComputer && currentPlayer==computerPlayer) return;
                    int col = colFromX(e.getX());
                    if(col>=0 && col<COLS){
                        makeMove(col);
                    }
                }
            });
        }

        void setGhostDisc(int r, int c, int p){ ghostR=r; ghostC=c; ghostPlayer=p; }
        void clearGhost(){ ghostR=-1; ghostC=-1; ghostPlayer=0; }
        void resetAnimation(){ clearGhost(); }

        private int colFromX(int x){
            int w = getWidth() - 2*padding;
            int cell = w / COLS;
            int left = padding;
            return (x - left) / cell;
        }

        @Override protected void paintComponent(Graphics g){
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg = darkTheme ? new Color(22,27,34) : new Color(240,240,240);
            Color boardBlue = darkTheme ? new Color(32,78,141) : new Color(33,97,171);
            Color empty = darkTheme ? new Color(200,200,200) : new Color(220,220,220);
            Color red = new Color(220, 50, 47);
            Color yellow = new Color(241, 196, 15);

            setBackground(bg);

            int w = getWidth() - 2*padding;
            int h = getHeight() - 2*padding;
            int size = Math.min(w, h);
            int left = (getWidth() - size)/2;
            int top = (getHeight() - size)/2;
            int cell = size / Math.max(COLS, ROWS);
            int boardW = cell * COLS;
            int boardH = cell * ROWS;

            g2.setColor(boardBlue);
            g2.fillRoundRect(left, top, boardW, boardH, 30, 30);

            for(int r=0;r<ROWS;r++){
                for(int c=0;c<COLS;c++){
                    int cx = left + c*cell;
                    int cy = top + r*cell;
                    g2.setColor(empty);
                    g2.fillOval(cx+6, cy+6, cell-12, cell-12);

                    int val = board[r][c];
                    if(val != 0){
                        g2.setColor(val==1 ? red : yellow);
                        g2.fillOval(cx+10, cy+10, cell-20, cell-20);
                    }
                }
            }

            if(ghostPlayer!=0 && ghostR>=0 && ghostC>=0){
                int cx = left + ghostC*cell;
                int cy = top + ghostR*cell;
                g2.setColor(ghostPlayer==1 ? red : yellow);
                g2.fillOval(cx+10, cy+10, cell-20, cell-20);
            }

            g2.dispose();
        }
    }
}
