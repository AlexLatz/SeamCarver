package ga.alexlatz.seamcarverapp;

import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.util.Arrays;

public class SeamCarver {
    private WritableImage image;
    private double[][] energy;

    public SeamCarver(final WritableImage image) {
        this.image = image;
        energy = new double[width()][height()];
        for (int x = 0; x < width(); x++) {
            for (int y = 0; y < height(); y++) {
                energy[x][y] = energy(x, y);
            }
        }
    }

    public int width() {
        return (int) image.getWidth();
    }

    public int height() {
        return (int) image.getHeight();
    }

    public WritableImage image() {
        return image;
    }

    public double energy(final int x, final int y) {
        if (x == 0 || x == width() - 1 || y == 0 || y == height() - 1)
            return 1000;
        PixelReader reader = image.getPixelReader();
        return Math.sqrt(colorDiff(reader.getArgb(x + 1, y), reader.getArgb(x - 1, y))
                + colorDiff(reader.getArgb(x, y + 1), reader.getArgb(x, y - 1)));
    }

    public int[][] findVerticalSeam(int num) {
        int[][] seams = new int[height()][num];
        boolean[][] usedPixels = new boolean[height()][width()];
        double[][] energyTo = new double[width()][height()];
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                if (y == 0)
                    energyTo[x][y] = 0;
                else
                    energyTo[x][y] = Double.POSITIVE_INFINITY;
            }
        }
        for (int y = 0; y < height(); y++) {
            for (int x = 0; x < width(); x++) {
                relax(energyTo, x, y, x, y + 1);
                relax(energyTo, x, y, x + 1, y + 1);
                relax(energyTo, x, y, x - 1, y + 1);
            }
        }
        for (int i = 0; i < num; i++) {
            double min = Double.POSITIVE_INFINITY;
            for (int x = 0; x < width(); x++) {
                if (energyTo[x][height() - 2] < min && !usedPixels[height() - 2][x]) {
                    min = energyTo[x][height() - 2];
                    seams[height() - 2][i] = x;
                }
            }
            seams[height() - 1][i] = seams[height() - 2][i];
            usedPixels[height() - 2][seams[height() - 2][i]] = true;
            usedPixels[height() - 1][seams[height() - 2][i]] = true;
        }
        for (int i = 0; i < num; i++) {
            for (int y = height() - 2; y > 0; y--) {
                double minSeam = Double.POSITIVE_INFINITY;
                if (seams[y][i] - 1 > 0) {
                    int left = leftMove(usedPixels, seams[y][i], y - 1);
                    if (left != -1 && energyTo[left][y - 1] < minSeam && !usedPixels[y - 1][left]) {
                        minSeam = energyTo[left][y - 1];
                        seams[y - 1][i] = left;
                    }
                }
                if (energyTo[seams[y][i]][y - 1] < minSeam && !usedPixels[y - 1][seams[y][i]]) {
                    minSeam = energyTo[seams[y][i]][y - 1];
                    seams[y - 1][i] = seams[y][i];
                }
                if (seams[y][i] + 1 < width()) {
                    int right = rightMove(usedPixels, seams[y][i], y - 1);
                    if (right != -1 && energyTo[right][y - 1] < minSeam && !usedPixels[y - 1][right]) {
                        seams[y - 1][i] = right;
                    }
                }
                usedPixels[y - 1][seams[y - 1][i]] = true;
            }
        }
        return seams;
    }

    private int leftMove(boolean[][] usedPixels, int x, int y) {
        for (int i = 1; i <= x; i++) {
            if (!usedPixels[y][x - i]) return x - i;
        }
        return -1;
    }

    private int rightMove(boolean[][] usedPixels, int x, int y) {
        for (int i = 1; i < usedPixels[0].length - x; i++) {
            if (!usedPixels[y][x + i]) return x + i;
        }
        return -1;
    }

    public int[][] findHorizontalSeam(int num) {
        transpose();
        final int[][] seam = findVerticalSeam(num);
        transpose();
        return seam;
    }

    public void removeVerticalSeam(final int[][] seam) {
        final WritableImage newPic = new WritableImage(width() - seam[0].length, height());
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = newPic.getPixelWriter();
        double[][] newEnergy = new double[width() - seam[0].length][height()];
        for (int y = 0; y < height(); y++) {
            Arrays.sort(seam[y]);
            int curRow = 0;
            for (int x = 0; x < width(); x++) {
                if (Arrays.binarySearch(seam[y], x) < 0) writer.setArgb(curRow++, y, reader.getArgb(x, y));
            }
        }
        image = newPic;
        for (int y = 0; y < energy[0].length; y++) {
            int curRow = 0;
            for (int x = 0; x < energy.length; x++) {
                if (Arrays.binarySearch(seam[y], x) < 0) newEnergy[curRow++][y] = energy[x][y];
                else {
                    if (curRow > 0) newEnergy[curRow - 1][y] = energy(curRow - 1, y);
                    if (curRow < newEnergy.length) newEnergy[curRow][y] = energy(curRow, y);
                }
            }
        }
        energy = newEnergy;
    }

    public void removeHorizontalSeam(final int[][] seam) {
        if (height() <= 1)
            throw new IllegalArgumentException();
        transpose();
        removeVerticalSeam(seam);
        transpose();
    }

    public void addVerticalSeam(final int[][] seam) {
        final WritableImage newPic = new WritableImage(width() + seam[0].length, height());
        PixelWriter writer = newPic.getPixelWriter();
        PixelReader reader = image.getPixelReader();
        double[][] newEnergy = new double[width() + seam[0].length][height()];
        for (int y = 0; y < height(); y++) {
            Arrays.sort(seam[y]);
            int curRow = 0;
            for (int x = 0; x < width(); x++) {
                int i = Arrays.binarySearch(seam[y], x);
                if (i < 0) writer.setArgb(curRow++, y, reader.getArgb(x, y));
                else {
                    int seamColor = reader.getArgb(seam[y][i], y);
                    int nextColor;
                    if (x < width() - 1) nextColor = reader.getArgb(seam[y][i] + 1, y);
                    else nextColor = reader.getArgb(seam[y][i] - 1, y);
                    int a = ((((seamColor >> 24) & 0xff) + ((nextColor >> 24) & 0xff)) / 2);
                    int r = ((((seamColor >> 16) & 0xff) + ((nextColor >> 16) & 0xff)) / 2);
                    int g = ((((seamColor >> 8) & 0xff) + ((nextColor >> 8) & 0xff)) / 2);
                    int newColor = ((((seamColor) & 0xff) + ((nextColor) & 0xff)) / 2);
                    newColor = newColor + (g << 8);
                    newColor = newColor + (r << 16);
                    newColor = newColor + (a << 24);
                    if (x < width() - 1) {
                        writer.setArgb(curRow++, y, seamColor);
                        writer.setArgb(curRow++, y, newColor);
                    } else {
                        writer.setArgb(curRow++, y, newColor);
                        writer.setArgb(curRow++, y, seamColor);
                    }
                }
            }
        }
        image = newPic;
        for (int y = 0; y < energy[0].length; y++) {
            int curRow = 0;
            for (int x = 0; x < energy.length; x++) {
                if (Arrays.binarySearch(seam[y], x) < 0) newEnergy[curRow++][y] = energy[x][y];
                else {
                    if (x != energy.length - 1) {
                        if (curRow > 0) newEnergy[curRow - 1][y] = energy(curRow - 1, y);
                        newEnergy[curRow][y] = energy(curRow, y);
                        if (curRow < newEnergy.length - 1) newEnergy[curRow + 1][y] = energy(curRow + 1, y);
                    } else {
                        newEnergy[curRow - 2][y] = energy(curRow - 2, y);
                        newEnergy[curRow - 1][y] = energy(curRow - 1, y);
                        newEnergy[curRow][y] = energy(curRow, y);
                    }
                    curRow += 2;
                }
            }
        }
        energy = newEnergy;
    }

    public void addHorizontalSeam(final int[][] seam) {
        transpose();
        addVerticalSeam(seam);
        transpose();
    }

    private void transpose() {
        final WritableImage flipped = new WritableImage(height(), width());
        final double[][] newEnergy = new double[height()][width()];
        PixelWriter writer = flipped.getPixelWriter();
        PixelReader reader = image.getPixelReader();
        for (int x = 0; x < width(); x++) {
            for (int y = 0; y < height(); y++) {
                writer.setArgb(y, x, reader.getArgb(x, y));
                if (x >= energy.length || y >= energy[0].length) newEnergy[y][x] = energy(x, y);
                else newEnergy[y][x] = energy[x][y];
            }
        }
        image = flipped;
        energy = newEnergy;
    }

    private void relax(double[][] energyTo, final int x, final int y, final int x2, final int y2) {
        if (x2 < 0 || y2 < 0 || x2 > width() - 1 || y2 > height() - 1)
            return;
        if (energyTo[x2][y2] > energyTo[x][y] + energy[x2][y2])
            energyTo[x2][y2] = energyTo[x][y] + energy[x2][y2];
    }

    private double colorDiff(final int c1, final int c2) {
        final double r = ((c1 >> 16) & 0xff) - ((c2 >> 16) & 0xff);
        final double g = ((c1 >> 8) & 0xff) - ((c2 >> 8) & 0xff);
        final double b = ((c1) & 0xff) - ((c2) & 0xff);
        return r * r + g * g + b * b;
    }
}