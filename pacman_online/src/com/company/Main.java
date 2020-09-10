package com.company;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Stream;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;

class Reader {
    int[] data;

    public Reader(Scanner scanner) {


        data = Arrays.stream(scanner.nextLine().split(" "))
                .mapToInt(Integer::parseInt).toArray();
        Scanner secondLine = new Scanner(scanner.nextLine());

        int X = secondLine.nextInt();
        int Y = secondLine.nextInt();
        int pacmanCount = secondLine.nextInt();
        int ghostCount = secondLine.nextInt();
        if (secondLine.hasNextLine()) {
            MainClass.log("\nGot message:" + secondLine.nextLine() + "\n");
        }

        char[][] fa = Stream.generate(() -> scanner.nextLine().substring(0, Y).toCharArray())
                .limit(X).toArray(char[][]::new);

        MainClass.fieldController = new FieldController(fa[0].length, fa.length);


        for (int i = 0; i < fa.length; i++) {
            for (int j = 0; j < fa[i].length; j++) {
                switch (fa[i][j]) {
                    case ' ':
                        MainClass.fieldController.fields[j][i] = Field.SPACE;
                        break;
                    case '+':
                        MainClass.fieldController.fields[j][i] = Field.ENERGIZER;
                        break;
                    case 'F':
                        MainClass.fieldController.fields[j][i] = Field.WALL;
                        break;
                    case '1':
                        MainClass.fieldController.fields[j][i] = Field.COIN;
                        break;
                    case 'G':
                        MainClass.fieldController.fields[j][i] = Field.GHOST_GATE;
                        break;
                    default:
                        MainClass.log("UNEXPECTED FIELD TYPE: " + fa[i][j]);
                }
            }
        }

        String[][] paa = Stream.generate(() -> scanner.nextLine().split(" "))
                .limit(pacmanCount).toArray(String[][]::new);

        MainClass.pacmanBots = new ArrayList<>();

        for (String[] aPaa : paa) {
            if (aPaa[1].equals("mlet")) {

                MainClass.pacman = new Pacman(
                        aPaa[1],
                        new Position(
                                Integer.parseInt(aPaa[3]),
                                Integer.parseInt(aPaa[2])
                        ),
                        Integer.parseInt(aPaa[4]),
                        Integer.parseInt(aPaa[5]),
                        aPaa[6],
                        false
                );

            } else {
                MainClass.pacmanBots.add(
                        new Pacman(
                                aPaa[1],
                                new Position(
                                        Integer.parseInt(aPaa[3]),
                                        Integer.parseInt(aPaa[2])
                                ),
                                Integer.parseInt(aPaa[4]),
                                Integer.parseInt(aPaa[5]),
                                aPaa[6],
                                false
                        )
                );
            }
        }

        String[][] ga = Stream.generate(() -> scanner.nextLine().split(" "))
                .limit(ghostCount).toArray(String[][]::new);
        MainClass.ghosts = new Ghost[ghostCount];

        for (int i = 0; i < ghostCount; i++) {
            MainClass.ghosts[i] = new Ghost(
                    ga[i][0].charAt(0),
                    new Position(
                            Integer.parseInt(ga[i][2]),
                            Integer.parseInt(ga[i][1])
                    ),
                    Integer.parseInt(ga[i][3]), // Eatable until
                    Integer.parseInt(ga[i][4])  // Stopped until
            );
        }
    }
}

class MainClass {

    public static Pacman pacman;
    public static ArrayList<Pacman> pacmanBots;
    public static Ghost[] ghosts;

    public static FieldController fieldController;
    public static int mennyiSzellemetEvettEbbenAGyorsitasban = 0;
    private static long startTime;
    static int tick = 0;

    public static void main(String[] args) {
        startTime = System.nanoTime();


        setDefaultUncaughtExceptionHandler((t, e) -> log(t + " ERROR: " + Arrays.toString(e.getStackTrace())));

        MainClass.log("******************************************************************************************");
        MainClass.log("MEMORY: " + Math.round(getMemoryMB()) + "MB");
        MainClass.log("TIME: " + getRunningTimeS() + "s");

        //previousGhosts = new Ghost[0];


        for (tick = 0; true; tick++) {
            MainClass.log("TICK: " + tick);
            MainClass.log("TIME: " + getRunningTimeS() + "s");
            Reader read = new Reader(new Scanner(System.in));

            if (read.data[2] == -1) {
                break;
            }

            // Konstruktorban nem lehet meghívni, mert akkor még nicsnenek ghostok
            pacman.calculateDistances();

            char dir = Directions.getBestDirection(Directions.getDirectionScores(tick, pacmanBots.toArray(new Pacman[0]), pacman, fieldController, ghosts));

            System.err.print(dir);
            if (fieldController.isThereEatableGhost(pacman, Directions.getPositionByDirection(pacman.position, dir))) {
                mennyiSzellemetEvettEbbenAGyorsitasban++;
            }

            char dir2 = ' ';

            if (pacman.isFast()) {
                // INFÓK FRISSÍTÉSE ************************************************************************************

                // Amin állt, ott már nincs coin/+
                fieldController.setFieldAt(pacman.position, Field.SPACE);

                for (Ghost ghost : ghosts) {
                    if (ghost.isEatable()) {
                        ghost.position = Directions.getPositionByDirection(ghost.position, Directions.getOppositeDirection(fieldController.getDirectionByPositions(ghost.position, pacman.position)));
                    } else {
                        ghost.position = Directions.getPositionByDirection(ghost.position, fieldController.getDirectionByPositions(ghost.position, pacman.position));
                    }
                }

                // Pacman firssítése
                pacman.position = Directions.getPositionByDirection(pacman.position, dir);
                fieldController.setFieldAt(pacman.position, Field.SPACE);

                pacman.fastUntil--;

                pacman.calculateDistances();

                dir2 = Directions.getBestDirection(Directions.getDirectionScores(tick, pacmanBots.toArray(new Pacman[0]), pacman, fieldController, ghosts));

            } else {
                mennyiSzellemetEvettEbbenAGyorsitasban = 0;
            }

            // Megoldás kiírása:
            System.out.println(String.format("%d %d %d %c %c", read.data[0], read.data[1], read.data[2], dir, dir2));

            MainClass.log("");
        }
    }

    public static void log(String text) {
        System.err.println(text);
        /*try {
            URL url = new URL("https://ambrusweb11.hu/pacman/log.php");
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST"); // PUT is another valid option
            http.setDoOutput(true);

            Map<String, String> arguments = new HashMap<>();
            arguments.put("message", text);
            StringJoiner sj = new StringJoiner("&");
            for (Map.Entry<String, String> entry : arguments.entrySet())
                sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                        + URLEncoder.encode(entry.getValue(), "UTF-8"));
            byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            http.connect();
            try (OutputStream os = http.getOutputStream()) {
                os.write(out);
            }
        } catch (Exception ignored) {

        }*/
    }

    public static double getRunningTimeS() {
        return (System.nanoTime() - startTime) / 1000000000.0;
    }

    public static double getMemoryMB() {
        return ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1000000.0);
    }
}

class Directions {
    public static final char RIGHT = '>';
    public static final char UP = '^';
    public static final char LEFT = '<';
    public static final char DOWN = 'v';

    public static char[] directions = new char[]{'>', '^', '<', 'v'};

    public static char getRandom() {
        Random random = new Random();
        return directions[random.nextInt(4)];
    }

    public static char getOppositeDirection(char direction) {
        switch (direction) {
            case RIGHT:
                return Directions.LEFT;
            case UP:
                return Directions.DOWN;
            case LEFT:
                return Directions.RIGHT;
            case DOWN:
                return Directions.UP;
            default:
                MainClass.log("DEFAULT CASE! DIR: " + direction);
                return Directions.RIGHT;
        }
    }

    public static char getBestDirection(double[] directionScores) {
        for (int i = 0; i < 4; i++) {
            MainClass.log(Directions.get(i) + " -> " + directionScores[i]);
        }
        double bestScore = -10000000;
        char bestDirection = '^';
        for (int i = 0; i < 4; i++) {
            if (directionScores[i] > bestScore) {
                bestScore = directionScores[i];
                bestDirection = Directions.get(i);
            }
        }
        return bestDirection;
    }

    public static Position getDeltaPosByDirection(char direction) {
        switch (direction) {
            case RIGHT:
                return new Position(1, 0);
            case UP:
                return new Position(0, -1);
            case LEFT:
                return new Position(-1, 0);
            case DOWN:
                return new Position(0, 1);
            default:
                MainClass.log("DEFAULT CASE! DIR: " + direction);
                return new Position(0, -1);
        }
    }

    public static Position getPositionByDirection(Position position, char direction) {
        Position deltaPos = getDeltaPosByDirection(direction);
        return MainClass.fieldController.checkTeleportPosition(new Position(position.x + deltaPos.x, position.y + deltaPos.y));
    }

    public static int getIndexByDirection(char dir) {
        for (int i = 0; i < directions.length; i++) {
            if (directions[i] == dir) return i;
        }
        return 0;
    }

    public static double[] getDirectionScores(int tick, Pacman[] pacmanBots, Pacman pacman, FieldController fieldController, Ghost[] ghosts) {
        double[] directionScores = new double[]{0, 0, 0, 0};

        // COINOK ÉRTÉKELÉSE *******************************************************************************************
        for (int i = 0; i < pacman.coins.length; i++) {
            int targetDir = getIndexByDirection(fieldController.getDirectionByPositions(pacman.position, pacman.coins[i]));
            directionScores[targetDir] += 20.0 / pacman.getDistance(pacman.coins[i]);
        }

        // ENERGIZER ***************************************************************************************************
        boolean isRemainingEnergizer = false;

        ArrayList<Position> energizers = new ArrayList<>();

        for (int j = 0; j < fieldController.getWidth(); j++) {
            for (int k = 0; k < fieldController.getHeight(); k++) {
                Position energizerPosition = new Position(j, k);
                if (fieldController.isThereEnergizer(energizerPosition)) {
                    isRemainingEnergizer = true;
                    energizers.add(energizerPosition);
                }
            }
        }
        if (isRemainingEnergizer) {
            int bestDirection = 0;
            int bestDistance = 10000;

            for (int i = 0; i < 4; i++) {
                for (Position energizer : energizers) {
                    if (!fieldController.isThereWall(getPositionByDirection(pacman.position, Directions.directions[i])) && pacman.getDistance(energizer) < bestDistance) {
                        bestDistance = pacman.getDistance(energizer);
                        bestDirection = fieldController.getDirectionIndexByPositions(pacman.position, energizer);
                    }
                }
            }
            directionScores[bestDirection] += 100;
        }

        // JUTALMAZÁS *********************************************************************************************
        for (int i = 0; i < 4; i++) {
            MainClass.log("irany " + Directions.get(i));

            Position newPos;
            newPos = Directions.getPositionByDirection(pacman.position, Directions.get(i));
            Pacman newPacman = new Pacman(newPos, pacman.fastUntil <= 0 ? 0 : pacman.fastUntil - 1, pacman.currentScore, pacman.plus, false);
            newPacman.calculateDistances();

            // Ha WALL-ra lépne ****************************************************************************************
            if (fieldController.isThereWall(newPos) || fieldController.isThereGhostGate(newPos)) {
                directionScores[i] -= 100000; // Nem jó
            }

            // FÉLÉS SZELLEMEKTŐL || SZELLEMEVÉS ***********************************************************************
            if (pacman.isFast()) {
                //MainClass.log("FAST");
                for (Ghost ghost : ghosts) {
                    // Ha ehető a szellem:
                    if (ghost.eatableUntil >= pacman.getDistance(pacman.position)) {
                        if (pacman.getDistance(ghost.position) < 12) {
                            if (!fieldController.isThereWall(newPos) && !fieldController.isThereGhostGate(newPos)) {
                                if (newPacman.getDistance(ghost.position) < pacman.getDistance(ghost.position)) {
                                    directionScores[i] += (1000 * Math.pow(2, MainClass.mennyiSzellemetEvettEbbenAGyorsitasban)) / newPacman.getDistance(ghost.position);
                                }
                            }
                        }
                    } else {  // Ha nem ehető a szellem: (félünk!!)
                        if (pacman.getDistance(ghost.position) < 4) {
                            if (!fieldController.isThereWall(newPos) && !fieldController.isThereGhostGate(newPos)) {
                                if (newPacman.getDistance(ghost.position) < 3) {
                                    // Ha életveszélybe kerülnénk:
                                    directionScores[i] -= 1000000;
                                }
                            }
                        }
                    }
                }
            } else { // Ha lassúk vagyunk
                for (Ghost ghost : ghosts) {
                    if (pacman.getDistance(ghost.position) < 4) {
                        if (!fieldController.isThereWall(newPos) && !fieldController.isThereGhostGate(newPos)) {
                            // Ha életveszélybe kerülnénk:
                            if (newPacman.getDistance(ghost.position) < 3) {
                                directionScores[i] -= 1000000;
                            }
                        }
                    }
                }
            }

            for (Pacman pacmanBot : pacmanBots) {
                if (pacmanBot.currentScore < pacman.currentScore) {
                    if (pacman.getDistance(pacmanBot.position) < 3) {
                        if (pacman.getDistance(pacmanBot.position) < 2) {
                            directionScores[i] -= ((double) pacman.currentScore - (double) pacmanBot.currentScore) / 4.0;
                        }
                    }
                }
            }
        }

        MainClass.log("MEMORY: " + Math.round(MainClass.getMemoryMB()) + "MB");

        return directionScores;
    }

    // Gets the nth direction
    public static char get(int i) {
        return directions[i];
    }
}

class Pacman {
    public String id;
    public int fastUntil, currentScore;
    public String plus;
    public Position position;

    int[][] distances;
    Position[] coins;

    Pacman(Position position, int fastUntil, Integer currentScore, String plus) {
        this.position = position;
        this.fastUntil = fastUntil;
        this.currentScore = currentScore;
        this.plus = plus;

        calculateDistances();
    }

    Pacman(Position position, int fastUntil, Integer currentScore, String plus, boolean calculateThings) {
        this.position = position;
        this.fastUntil = fastUntil;
        this.currentScore = currentScore;
        this.plus = plus;

        if (calculateThings) {
            calculateDistances();
        }
    }

    Pacman(String id, Position position, int fastUntil, int currentScore, String plus) {
        this.id = id;
        this.position = position;
        this.fastUntil = fastUntil;
        this.currentScore = currentScore;
        this.plus = plus;

        calculateDistances();
    }

    Pacman(String id, Position position, int fastUntil, int currentScore, String plus, boolean calculateThings) {
        this.id = id;
        this.position = position;
        this.fastUntil = fastUntil;
        this.currentScore = currentScore;
        this.plus = plus;

        if (calculateThings) {
            calculateDistances();
        }
    }

    void calculateDistances() {
        coins = new Position[4];
        int coinCount = 0;

        distances = new int[MainClass.fieldController.getWidth()][MainClass.fieldController.getHeight()];
        ArrayList<Position> knownDistancePositions = new ArrayList<>();
        for (int i = 0; i < MainClass.fieldController.getWidth(); i++) {
            for (int j = 0; j < MainClass.fieldController.getHeight(); j++) {
                distances[i][j] = -1;
            }
        }

        distances[this.position.x][this.position.y] = 0;


        knownDistancePositions.add(position);
        int d = 1;
        boolean allChanged = false;
        while (!allChanged) {
            allChanged = true;
            ArrayList<Position> newKnownDistancePositions = new ArrayList<>();
            for (Position position : knownDistancePositions) {
                for (char direction : Directions.directions) {
                    Position newPosition = Directions.getPositionByDirection(position, direction);
                    newPosition = MainClass.fieldController.checkTeleportPosition(newPosition);
                    boolean wall = MainClass.fieldController.isThereWall(newPosition) || MainClass.fieldController.isThereGhostGate(newPosition);
                    if (!wall && distances[newPosition.x][newPosition.y] == -1) {
                        distances[newPosition.x][newPosition.y] = d;
                        if (MainClass.fieldController.isThereCoin(newPosition) && coinCount != 4) {
                            coins[coinCount] = newPosition;
                            coinCount++;
                        }
                        newKnownDistancePositions.add(newPosition);
                        allChanged = false;
                    }
                }
            }
            knownDistancePositions = newKnownDistancePositions;
            d++;
        }
    }

    int getDistance(Position position) {
        int distance = distances[position.x][position.y];
        //if (this.isFast()) return (int) Math.floor((double) distance / 2.0) + 1;
        return distance;
    }

    public boolean isFast() {
        return fastUntil > 0;
    }

    @Override
    public String toString() {
        return "Pacman{" +
                "  position: " + position.toString() +
                ", fastUntil=" + fastUntil +
                ", currentScore=" + currentScore +
                ", plus='" + plus + '\'' +
                '}';
    }

    static int wallCount(Position position) {
        if (MainClass.fieldController.isThereWall(position)) {
            return -1;
        }
        int wallSum = 0;
        for (int i = 0; i < 4; i++) {
            Position pos = Directions.getPositionByDirection(position, Directions.get(i));
            if (MainClass.fieldController.isThereWall(pos) ||
                    MainClass.fieldController.isThereGhostGate(pos)) {
                wallSum++;
            }
        }
        return wallSum;
    }
}

class Ghost {
    char ghostId;
    int eatableUntil, stoppedUntil;
    int[][] distances;

    Position position;

    public Ghost(char ghostId,
                 Position position,
                 int eatableUntil,
                 int stoppedUntil) {
        this.ghostId = ghostId;
        this.position = position;
        this.eatableUntil = eatableUntil;
        this.stoppedUntil = stoppedUntil;
    }


    public boolean isEatable() {
        return this.eatableUntil > 0;
    }

    @Override
    public String toString() {
        return "Ghost{" +
                "ghostId=" + ghostId +
                ", eatableUntil=" + eatableUntil +
                ", stoppedUntil=" + stoppedUntil +
                ", distances=" + Arrays.toString(distances) +
                ", position=" + position +
                '}';
    }

    int wallCount(Position position) {
        int wallSum = 0;
        for (int i = 0; i < 4; i++) {
            if (MainClass.fieldController.isThereWall(Directions.getPositionByDirection(position, Directions.get(i)))) {
                wallSum++;
            }
        }
        return wallSum;
    }
}

class FieldController {

    Field[][] fields;

    FieldController(int width, int height) {
        this.fields = new Field[width][height];
    }

    @Override
    public String toString() {
        return "FieldController{" +
                "fields=" + MyMath.matrixOut(fields) +
                '}';
    }


    void setFieldAt(Position position, Field field) {
        position = checkTeleportPosition(position);
        fields[position.x][position.y] = field;
    }

    boolean isThereGhost(Position position) {
        position = checkTeleportPosition(position);
        for (Ghost ghost : MainClass.ghosts) {
            if (ghost.eatableUntil == 0 && ghost.position.equals(position)) {
                return true;
            }
        }
        return false;
    }

    boolean isThereEatableGhost(Pacman pacman, Position position) {
        position = checkTeleportPosition(position);
        for (Ghost ghost : MainClass.ghosts) {
            if (pacman.isFast() && ghost.eatableUntil >= 1 && ghost.position.equals(position)) {
                return true;
            }
        }
        return false;
    }

    boolean isThereWall(Position position) {
        position = checkTeleportPosition(position);
        return fields[position.x][position.y] == Field.WALL;
    }

    boolean isThereGhostGate(Position position) {
        position = checkTeleportPosition(position);
        return fields[position.x][position.y] == Field.GHOST_GATE;
    }

    boolean isThereCoin(Position position) {
        position = checkTeleportPosition(position);
        return fields[position.x][position.y] == Field.COIN;
    }

    boolean isThereEnergizer(Position position) {
        position = checkTeleportPosition(position);
        /*if (fields[position.x][position.y] == Field.ENERGIZER) {
            boolean closest = true;
            for (Pacman pacmanBot : MainClass.pacmanBots) {
                if (pacmanBot.distances == null) pacmanBot.calculateDistances();
                if (pacmanBot.getDistance(position) < MainClass.pacman.getDistance(position)) {
                    closest = false;
                }
            }
            return closest;
        }
        return false;*/
        return fields[position.x][position.y] == Field.ENERGIZER;
    }

    public int getWidth() {
        return fields.length;
    }

    public int getHeight() {
        return fields[0].length;
    }

    public Position checkTeleportPosition(Position position) {
        if (position.x < 0) {
            position.x = MainClass.fieldController.getWidth() - 1;
        } else if (position.x > MainClass.fieldController.getWidth() - 1) {
            position.x = 0;
        }
        if (position.y < 0) {
            position.y = MainClass.fieldController.getHeight() - 1;
        } else if (position.y > MainClass.fieldController.getHeight() - 1) {
            position.y = 0;
        }
        return position;
    }

    char getDirectionByPositions(Position pacPosition, Position position2) {
        pacPosition = checkTeleportPosition(pacPosition);
        position2 = checkTeleportPosition(position2);
        Pacman origo = new Pacman(
                position2,
                MainClass.pacman.fastUntil - 1,
                0,
                "XXXX",
                false
        );

        origo.calculateDistances();
        Integer bestDistance = null;
        char bestDirection = '<';
        for (int i = 0; i < 4; i++) {
            Position newPosition = Directions.getPositionByDirection(pacPosition, Directions.get(i));
            newPosition = checkTeleportPosition(newPosition);
            if (isThereWall(newPosition) || isThereGhostGate(newPosition)) {
                continue;
            }
            if (bestDistance == null || origo.getDistance(newPosition) < bestDistance) {
                bestDistance = origo.getDistance(newPosition);
                bestDirection = Directions.get(i);
            }
        }
        return bestDirection;
    }

    int getDirectionIndexByPositions(Position pacPosition, Position position2) {
        pacPosition = checkTeleportPosition(pacPosition);
        position2 = checkTeleportPosition(position2);
        Pacman origo = new Pacman(
                position2,
                MainClass.pacman.fastUntil - 1,
                0,
                "XXXX",
                false
        );

        origo.calculateDistances();
        Integer bestDistance = null;
        int bestDirection = 100;
        for (int i = 0; i < 4; i++) {
            Position newPosition = Directions.getPositionByDirection(pacPosition, Directions.get(i));
            newPosition = checkTeleportPosition(newPosition);
            if (isThereWall(newPosition) || isThereGhostGate(newPosition)) {
                continue;
            }
            if (bestDistance == null || origo.getDistance(newPosition) < bestDistance) {
                bestDistance = origo.getDistance(newPosition);
                bestDirection = i;
            }
        }
        return bestDirection;
    }
}

enum Field {
    WALL, SPACE, COIN, ENERGIZER, GHOST_GATE
}

class Position {
    int x;
    int y;

    Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Position)) {
            return false;
        }
        Position obj2 = (Position) obj;

        return obj2.x == this.x && obj2.y == this.y;
    }

    Position copy() {
        return new Position(x, y);
    }
}

class MyMath {
    static int max(int[] array) {
        Integer max = null;
        for (int item : array) {
            if (max == null || item > max) {
                max = item;
            }
        }
        return max == null ? 0 : max;
    }

    static int min(int[] array) {
        Integer min = null;
        for (int item : array) {
            if (min == null || item < min) {
                min = item;
            }
        }
        return min == null ? 0 : min;
    }

    static boolean arrayContains(char[] array, char item) {
        for (char item_ : array) {
            if (item == item_) return true;
        }
        return false;
    }

    static boolean arrayContains(boolean[] array, boolean item) {
        for (boolean item_ : array) {
            if (item == item_) return true;
        }
        return false;
    }

    static boolean arrayContainsById(Ghost[] array, Ghost item) {
        for (Ghost item_ : array) {
            if (item.ghostId == item_.ghostId) return true;
        }
        return false;
    }

    static Ghost getGhostById(Ghost[] ghosts, char id) {
        for (Ghost ghost : ghosts) {
            if (ghost.ghostId == id) return ghost;
        }
        return null;
    }

    static String matrixOut(int[][] matrix) {
        return Arrays.deepToString(matrix)
                .replace('[', '{')
                .replace(']', '}');
    }

    static String matrixOut(Field[][] matrix) {
        return Arrays.deepToString(matrix)
                .replace('[', '{')
                .replace(']', '}');
    }

    static String matrixOut(boolean[][] matrix) {
        return Arrays.deepToString(matrix)
                .replace('[', '{')
                .replace(']', '}')
                .replace("true", "1")
                .replace("false", "0");
    }
}