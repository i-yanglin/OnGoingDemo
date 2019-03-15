
package com.example.yanglin.ongoingdemo1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by yanglin on 2018/11/27.
 */

public class DiscreteFrechetDistance {

    /** Scanner instance */

    /** Dimensions of the time series */
    private static int DIM;
    /** Dynamic programming memory array */
    private static double[][] mem;
    /** First time series */
    private static List<Point> timeSeriesP;
    /** Second time series */
    private static List<Point> timeSeriesQ;

    public String createStr(File f1){
        String str = null;
        try{
            BufferedReader in = new BufferedReader(new FileReader(f1));
            while((str = in.readLine())!=null){
                return str;
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return str;
    }

    public String test(String xSeries,String ySeries){
        long startTime = System.currentTimeMillis();
        timeSeriesP = parseInput(xSeries);
        timeSeriesQ = parseInput(ySeries);

        DecimalFormat df = new DecimalFormat("#00.0000");

        double res =  computeDiscreteFrechet(timeSeriesP, timeSeriesQ);
        String s = df.format(res);
        // how long it took to process
        long runTime = System.currentTimeMillis() - startTime;
        System.out.println("The Discrete Frechet Distance between these two time series is " + s + ". (" + runTime + " ms)");
return s;

    }

    private static double computeDiscreteFrechet(List<Point> P, List<Point> Q) {

        mem = new double[P.size()][Q.size()];

        // initialize all values to -1
        for (int i = 0; i < mem.length; i++) {
            for (int j = 0; j < mem[i].length; j++) {
                mem[i][j] = -1.0;
            }
        }

        return computeDFD(P.size() - 1, Q.size() - 1);
    }
    private static double computeDFD(int i, int j) {

        // if the value has already been solved
        if (mem[i][j] > -1)
            return mem[i][j];
            // if top left column, just compute the distance
        else if (i == 0 && j == 0)
            mem[i][j] = euclideanDistance(timeSeriesP.get(i), timeSeriesQ.get(j));
            // can either be the actual distance or distance pulled from above
        else if (i > 0 && j == 0)
            mem[i][j] = max(computeDFD(i - 1, 0), euclideanDistance(timeSeriesP.get(i), timeSeriesQ.get(j)));
            // can either be the distance pulled from the left or the actual
            // distance
        else if (i == 0 && j > 0)
            mem[i][j] = max(computeDFD(0, j - 1), euclideanDistance(timeSeriesP.get(i), timeSeriesQ.get(j)));
            // can be the actual distance, or distance from above or from the left
        else if (i > 0 && j > 0) {
            mem[i][j] = max(min(computeDFD(i - 1, j), computeDFD(i - 1, j - 1), computeDFD(i, j - 1)), euclideanDistance(timeSeriesP.get(i), timeSeriesQ.get(j)));
        }
        // infinite
        else
            mem[i][j] = Double.MAX_VALUE;

        // printMemory();
        // return the DFD
        return mem[i][j];
    }
    private static double max(double... values) {
        double max = Double.MIN_VALUE;
        for (double i : values) {
            if (i >= max)
                max = i;
        }
        return max;
    }
    private static double min(double... values) {
        double min = Double.MAX_VALUE;
        for (double i : values) {
            if (i <= min)
                min = i;
        }
        return min;
    }
    private static double euclideanDistance(Point i, Point j) {

        double distance = 0;
        // for each dimension
        for (int n = 0; n < DIM; n++) {
            distance += Math.sqrt(Math.pow(Math.abs((i.dimensions[n] - j.dimensions[n])), 2));
        }

        return distance;
    }

    private static List<Point> parseInput(String input) {
        List<Point> points = new ArrayList<Point>();

        // split the input string up by semi-colon
        String[] tuples = input.split(";");
        if (tuples != null && tuples.length > 0) {

            // for each tuple pair
            for (String tup : tuples) {
                // get the dimension of each
                String[] dims = tup.split(",");

                // if valid split
                if (dims != null && dims.length > 0) {

                    // construct new array of dims.length dimensions
                    // int[] dimensions = new int[dims.length];
                    double[] dimensions = new double[dims.length];
                    // set the global dimensional value
                    if (DIM != dims.length)
                        DIM = dims.length;

                    // for each dimension
                    for (int i = 0; i < dims.length; i++) {
                        //int val = Integer.parseInt(dims[i]);
                        double val = Double.parseDouble(dims[i]);
                        dimensions[i] = val;
                    }

                    Point p = new Point(dimensions);
                    // add the point to list of points
                    points.add(p);
                    Arrays.toString(p.dimensions);
                }
            }
        }

        return points;
    }

    /**
     * Test method that prints the 2D dynamic programming array.
     */
    private static void printMemory() {
        System.out.println("\n\n");
        for (int row = 0; row < mem.length; row++) {
            for (int col = 0; col < mem[row].length; col++) {
                System.out.print(mem[row][col] + "\t");
            }
            System.out.println();
        }
    }
}
