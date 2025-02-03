/*
 * Copyright (c) 2017-2024. Robin Hillyard
 */

package com.phasmidsoftware.dsaipg.misc;

class NewtonApproximation {
    public static void main(String[] args) {
        // Newton's Approximation to solve f(x)=x*x-2 =0
        double x = 1.0;
        int left = 200;
        for (; left > 0; left--) {
            final double y = Math.pow(x, 2)-2;
            if (Math.abs(y) < 1E-7) {
                System.out.println("the solution to x*x -2 is: " + x);
                System.exit(0);
            }
            x = x - y / (2*x);
            //Xn+1 =Xn -f(x)/f'(x)
            // Xn+1 =Xn -y/y'
        }
    }
}