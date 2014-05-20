/*
 * The MIT License
 *
 * Copyright (c) 2011 The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.sf.picard.util;

import java.math.BigDecimal;
import java.util.Arrays;

import static java.lang.Math.pow;

/**
 * General math utilities
 *
 * @author Tim Fennell
 */
public class MathUtil {
    /** The double value closest to 1 while still being less than 1. */
    public static final double MAX_PROB_BELOW_ONE = 0.9999999999999999d;

    /** Calculated the mean of an array of doubles. */
    public static double mean(final double[] in, final int start, final int stop) {
        double total = 0;
        for (int i = start; i < stop; ++i) {
            total += in[i];
        }

        return total / (stop - start);
    }

    /** Calculated the standard deviation of an array of doubles. */
    public static double stddev(final double[] in, final int start, final int length) {
        return stddev(in, start, length, mean(in, start, length));
    }

    /** Calculated the standard deviation of an array of doubles. */
    public static double stddev(final double[] in, final int start, final int stop, final double mean) {
        double total = 0;
        for (int i = start; i < stop; ++i) {
            total += (in[i] * in[i]);
        }

        return Math.sqrt((total / (stop - start)) - (mean * mean));
    }

    public static int compare(final int v1, final int v2) {
        return (v1 < v2 ? -1 : (v1 == v2 ? 0 : 1));
    }

    /** Calculate the median of an array of doubles. Assumes that the input is sorted */
    public static double median(final double... in) {
        if (in.length == 0) {
            throw new IllegalArgumentException("Attempting to find the median of an empty array");
        }

        final double[] data = Arrays.copyOf(in, in.length);
        Arrays.sort(data);
        final int middle = data.length / 2;
        return data.length % 2 == 1 ? data[middle] : (data[middle - 1] + data[middle]) / 2.0;
    }

    /**
     * Obtains percentage of two Longs
     * @param numerator   dividend
     * @param denominator divisor
     * @return numerator/(double)denominator if both are non-null and denominator != 0, else returns null.
     */
    public static Double percentageOrNull(final Long numerator, final Long denominator) {
        if (numerator != null && denominator != null && denominator != 0) {
            return numerator.doubleValue() / denominator.doubleValue();
        } else {
            return null;
        }
    }

    /** 
     * Round off the value to the specified precision. 
     */
    public static double round(final double num, final int precision) {
        BigDecimal bd = new BigDecimal(num);
        bd = bd.setScale(precision, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }

    /** Returns the largest value stored in the array. */
    public static double max(final double[] nums) {
        return nums[indexOfMax(nums)];
    }

    /**
     * Returns the index of the largest element in the array.  If there are multiple equal maxima then
     * the earliest one in the array is returned.
     */
    public static int indexOfMax(final double[] nums) {
        double max = nums[0];
        int index  = 0;
        for (int i = 1; i < nums.length; ++i) {
            if (nums[i] > max) {
                max = nums[i];
                index = i;
            }
        }

        return index;
    }

    /** Returns the smallest value stored in the array. */
    public static double min(final double[] nums) {
        double min = nums[0];
        for (int i = 1; i < nums.length; ++i) {
            if (nums[i] < min) min = nums[i];
        }

        return min;
    }

    /** Mimic's R's seq() function to produce a sequence of equally spaced numbers. */
    public static double[] seq(final double from, final double to, final double by) {
        if (from < to && by <= 0) return new double[0];
        if (from > to && by >= 0) return new double[0];
        final int values = 1 + (int) Math.floor((to - from) / by);
        final double[] results = new double[values];

        BigDecimal value = new BigDecimal(from);
        BigDecimal increment = new BigDecimal(by);

        for (int i=0; i<values; ++i) {
            results[i] = value.doubleValue();
            value = value.add(increment);
        }

        return results;
    }

    /** "Promotes" an int[] into a double array with the same values (or as close as precision allows). */
    public static double[] promote(final int[] is) {
        final double[] ds = new double[is.length];
        for (int i = 0; i < is.length; ++i) ds[i] = is[i];
        return ds;
    }

    /**
     * Takes a complete set of mutually exclusive log likelihoods and converts them to probabilities
     * that sum to 1 with as much fidelity as possible.  Limits probabilities to be in the space:
     * 0.9999999999999999 >= p >= (1-0.9999999999999999)/(likelihoods.length-1)
     */
    public static double[] logLikelihoodsToProbs(final double[] likelihoods) {
        // Note: bumping all the LLs so that the biggest is 300 ensures that we have the
        // widest range possible when unlogging them before one of them underflows. 10^300 is
        // near the maximum before you hit positive infinity.

        final double maxLikelihood = max(likelihoods);
        final double bump = 300 - maxLikelihood;

        final double[] tmp = new double[likelihoods.length];
        double total = 0;
        for (int i = 0; i < likelihoods.length; ++i) {
            tmp[i] = pow(10, likelihoods[i] + bump);
            total += tmp[i];
        }

        final double maxP = MAX_PROB_BELOW_ONE;
        final double minP = (1 - MAX_PROB_BELOW_ONE) / (tmp.length - 1);

        for (int i = 0; i < likelihoods.length; ++i) {
            tmp[i] /= total;
            if (tmp[i] > maxP) tmp[i] = maxP;
            else if (tmp[i] < minP) tmp[i] = minP;
        }

        return tmp;
    }

    /** Calculates the product of two arrays of the same length. */
    public static double[] multiply(final double[] lhs, final double[] rhs) {
        if (lhs.length != rhs.length) throw new IllegalArgumentException("Arrays must be of same length.");

        final int len = lhs.length;
        final double[] result = new double[len];
        for (int i = 0; i < len; ++i) result[i] = lhs[i] * rhs[i];
        return result;
    }

    /** Returns the sum of the elements in the array. */
    public static double sum(final double[] arr) {
        double result = 0;
        for (final double next : arr) result += next;
        return result;
    }

    /** Returns the sum of the elements in the array starting with start and ending before stop. */
    public static long sum(final long[] arr, final int start, final int stop) {
        long result = 0;
        for (int i=start; i<stop; ++i) {
            result += arr[i];
        }
        return result;
    }

    public static final LogMath LOG_2_MATH = new LogMath(2);
    public static final LogMath NATURAL_LOG_MATH = new LogMath(Math.exp(1)) {
        @Override
        public double getLogValue(final double nonLogValue) {
            return Math.log(nonLogValue);
        }
    };

    public static final LogMath LOG_10_MATH = new LogMath(10) {
        @Override
        public double getLogValue(final double nonLogValue) {
            return Math.log10(nonLogValue);
        }
    };
    
    /** 
     * A collection of common math operations that work with log values. To use it, pass values from log space, the operation will be
     * computed in non-log space, and a value in log space will be returned.
     */
    public static class LogMath {
        private final double base;

        private LogMath(final double base) {
            this.base = base;
        }

        /** Returns the decimal representation of the provided log values. */
        public double getNonLogValue(final double logValue) {
            return Math.pow(base, logValue);
        }

        /** Returns the log-representation of the provided decimal value. */
        public double getLogValue(final double nonLogValue) {
            return Math.log(nonLogValue) / Math.log(base);
        }

        /** Computes the mean of the provided log values. */
        public double mean(final double... logValues) {
            return sum(logValues) - getLogValue(logValues.length);
        }
        
        /** Computes the sum of the provided log values. */
        public double sum(final double... logValues) {
            // Avoid overflow via scaling.
            final double scalingFactor = max(logValues);
            double simpleAdditionResult = 0;
            for (final double v : logValues) {
                simpleAdditionResult += getNonLogValue(v - scalingFactor);
            }
            return getLogValue(simpleAdditionResult) + scalingFactor;
        }
 
        /** Computes the sum of the provided log values. */
        public double product(final double... logValues) {
            return MathUtil.sum(logValues);
        }
    }
}
