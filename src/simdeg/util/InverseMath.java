package simdeg.util;

import flanagan.analysis.Stat;

final class InverseMath {

    /**
     * Computes the inverse of the standard normal cumulative distribution function.
     * The algorithm uses two separate rational minimax approximations.
     */
    public static final double inverseStandardNormal(double p) {
        final double[] COEFF1 = { -3.969683028665376E1d, 2.209460984245205E2d,
            -2.759285104469687E2, 1.383577518672690E2d,
            -3.066479806614716E1d, 2.506628277459239d };
        final double[] COEFF2 = { -5.447609879822406E1d, 1.615858368580409E2d,
            -1.556989798598866E2d, 6.680131188771972E1d, -1.328068155288572E1d };
        final double[] COEFF3 = { -7.784894002430293E-3d, -3.223964580411365E-1d,
            -2.400758277161838d,-2.549732539343734d,
            4.374664141464968d, 2.938163982698783d };
        final double[] COEFF4 = { 7.784695709041462E-3d, 3.224671290700398E-1d,
            2.445134137142996d, 3.754408661907416d };

         final double BREAK_LEVEL = 0.02425d;

        /* Test for admissibility of parameters */
        if (p < 0.0d || p > 1.0d)
            throw new OutOfRangeException(p, 0.0d, 1.0d);

        /* Optimization */
        if (p == 0.0d)
            return Double.NEGATIVE_INFINITY;
        else if (p == 1.0d)
            return Double.POSITIVE_INFINITY;

        if (Math.min(p, 1.0d - p) < BREAK_LEVEL) {
            /* Rational approximation for lower and upper regions */
            final double q = Math.sqrt(-2.0d * Math.log(Math.min(p, 1.0d - p)));
            final double result = (((((COEFF3[0] * q + COEFF3[1]) * q + COEFF3[2]) * q + COEFF3[3]) * q + COEFF3[4]) * q + COEFF3[5])
                / ((((COEFF4[0] * q + COEFF4[1]) * q + COEFF4[2]) * q + COEFF4[3]) * q + 1.0d);
            return (p < 0.5d) ? result : - result;
        } else {
            /* Rational approximation for central region */
            final double q = p - 0.5d;
            final double r = q * q;
            return (((((COEFF1[0] * r + COEFF1[1]) * r + COEFF1[2]) * r + COEFF1[3]) * r + COEFF1[4]) * r + COEFF1[5]) * q /
                (((((COEFF2[0] * r + COEFF2[1]) * r + COEFF2[2]) * r + COEFF2[3]) * r + COEFF2[4]) * r + 1);
        }
    }

    /**
     * Computes the inverse of the normal cumulative distribution function.
     */
    public static final double inverseNormal(double p, double mean, double variance) {
        /* Test for admissibility of parameters */
        if (variance < 0.0d)
            throw new OutOfRangeException(variance, 0.0d, Double.MAX_VALUE);

        final double stdDev = Math.sqrt(variance);
        return mean + stdDev * inverseStandardNormal(p);
    }

    /**
     * Computes the inverse of the incomplete Beta function.
     */
    public static final double inverseIncompleteBeta(double alpha, double p, double q) {
        final double CONST1 = 2.30753d;
        final double CONST2 = 0.27061d;
        final double CONST3 = 0.99229d;
        final double CONST4 = 0.04481d;

        /* Test for admissibility of parameters */
        if (p < 0.0d)
            throw new OutOfRangeException(p, 0.0d, Double.MAX_VALUE);
        if (q < 0.0d)
            throw new OutOfRangeException(q, 0.0d, Double.MAX_VALUE);
        if (alpha < 0.0d || alpha > 1.0d)
            throw new OutOfRangeException(alpha, 0.0d, 1.0d);

        /* Optimization */
        if (alpha == 0.0d || alpha == 1.0d)
            return alpha;
        if (p == 1.0d && q == 1.0d)
            return alpha;
        if (p == 0.0d || q == 0.0d)
            return alpha < 0.5d ? 0.0d : 1.0d;
        if (p == 0.0d)
            return 0.0d;
        if (q == 0.0d)
            return 1.0d;

        /* Initialize */
        final double logbeta = logBeta(p, q);

        /* Change tail if necessary;  afterwards   0 < a <= 1/2 */
        final boolean swap_tail;
        final double a, pp, qq;
        if (alpha <= 0.5d) {
            a = alpha; pp = p; qq = q; swap_tail = false;
        } else { /* change tail, swap  p <-> q :*/
            a = 1.0d - alpha;
            pp = q; qq = p; swap_tail = true;
        }

        /* Calculate the initial approximation */
        double r = Math.sqrt(-2.0d * Math.log(a));
        double y = r - (CONST1 + CONST2 * r) / (1.0d + (CONST3 + CONST4 * r) * r);
        double xinbta;
        double h, s, t;

        if (pp > 1.0d && qq > 1.0d) {
            r = (y * y - 3.0d) / 6.0d;
            s = 1.0d / (pp + pp - 1.0d);
            t = 1.0d / (qq + qq - 1.0d);
            h = 2.0d / (s + t);
            final double w = y * Math.sqrt(h + r) / h - (t - s) * (r + 5.0d / 6.0d - 2.0d / (3.0d * h));
            xinbta = pp / (pp + qq * Math.exp(w + w));
        } else {
            r = qq + qq;
            t = 1.0d / (9.0d * qq);
            t = r * Math.pow(1.0d - t + y * Math.sqrt(t), 3.0d);
            if (t <= 0.0d)
                xinbta = 1.0d - Math.exp((Math.log1p(-a) + Math.log(qq) + logbeta) / qq);
            else {
                t = (4.0d * pp + r - 2.0d) / t;
                if (t <= 1.0d)
                    xinbta = Math.exp((Math.log(a * pp) + logbeta) / pp);
                else
                    xinbta = 1.0d - 2.0d / (t + 1.0d);
            }
        }

        /* Sometimes the approximation is negative */
        if (xinbta <= 0.0d || xinbta >= 1.0d)
            xinbta = 0.5d;

        /* Solve for x by a modified newton-raphson method, using the incomplete Beta function */
        double acu = Math.max(Double.MIN_VALUE, Math.pow(10.0d, -13.0d - 2.5d / (pp * pp) - 0.5d / (a * a)));
        double tx = 0.0d, prev = 0.0d;
        double yprev = 0.0d;
        double adj = 1.0d;
        r = 1.0d - pp;
        t = 1.0d - qq;
        for (int i_pb=0; i_pb < 1000; i_pb++) {
            y = Stat.regularisedBetaFunction(pp, qq, xinbta);
            if (Double.isInfinite(y))
                return Double.NaN;

            y = (y - a) *
                Math.exp(logbeta + r * Math.log(xinbta) + t * Math.log1p(-xinbta));
            if (y * yprev <= 0.0d)
                prev = Math.max(Math.abs(adj), Double.MIN_VALUE);
            double g = 1.0d;
            for (int i_inn=0; i_inn < 1000; i_inn++) {
                adj = g * y;
                if (Math.abs(adj) < prev) {
                    tx = xinbta - adj;
                    if (tx >= 0.0d && tx <= 1.0d) {
                        if (prev <= acu)
                            return swap_tail ? 1.0d - xinbta : xinbta;
                        if (Math.abs(y) <= acu)
                            return swap_tail ? 1.0d - xinbta : xinbta;
                        if (tx != 0.0d && tx != 1.0d)
                            break;
                    }
                }
                g /= 3.0d;
            }
            if (Math.abs(tx - xinbta) < 1e-15*xinbta)
                return swap_tail ? 1.0d - xinbta : xinbta;
            xinbta = tx;
            yprev = y;
        }

        /* No convergence: iteration count */
        throw new RuntimeException("Unable to converge while iterating");
    }

    public static final double logBeta(double p, double q) {
        /* Test for admissibility of parameters */
        if (p <= 0.0d)
            throw new OutOfRangeException(p, 0.0d, Double.MAX_VALUE);
        if (q <= 0.0d)
            throw new OutOfRangeException(q, 0.0d, Double.MAX_VALUE);
        return Stat.logGammaFunction(p) + Stat.logGammaFunction(q)
            - Stat.logGammaFunction(p+q);
    }

}
