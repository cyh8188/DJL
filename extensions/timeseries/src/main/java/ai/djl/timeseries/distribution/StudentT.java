package ai.djl.timeseries.distribution;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDManager;
import ai.djl.util.Preconditions;

/**
 * Student's t-test distribution.
 *
 * <p>Three arguments for this distribution. {@code mu} mean of the distribution, {@code sigma} the
 * standard deviations (scale), {@code nu} degrees of freedom.
 */
public class StudentT extends Distribution {

    private NDArray mu;
    private NDArray sigma;
    private NDArray nu;

    StudentT(Builder builder) {
        mu = builder.distrArgs.get("mu");
        sigma = builder.distrArgs.get("sigma");
        nu = builder.distrArgs.get("nu");
    }

    /** {@inheritDoc} */
    @Override
    public NDArray logProb(NDArray target) {
        NDArray nup1Half = nu.add(1.).div(2.);
        NDArray part1 = nu.getNDArrayInternal().rdiv(1.).mul(target.sub(mu).div(sigma).square());

        NDArray z =
                nup1Half.gammaln()
                        .sub(nu.div(2.).gammaln())
                        .sub(nu.mul(Math.PI).log().mul(0.5))
                        .sub(sigma.log());

        return z.sub(nup1Half.mul(part1.add(1.).log()));
    }

    /** {@inheritDoc} */
    @Override
    public NDArray sample(int numSamples) {
        NDManager manager = mu.getManager();
        NDArray expandedMu = mu.expandDims(0).repeat(0, numSamples);
        NDArray expandedSigma = sigma.expandDims(0).repeat(0, numSamples);
        NDArray expandedNu = nu.expandDims(0).repeat(0, numSamples);

        NDArray gammas =
                manager.sampleGamma(
                        expandedNu.div(2.),
                        expandedNu.mul(expandedSigma.square()).getNDArrayInternal().rdiv(2.));
        return manager.sampleNormal(expandedMu, gammas.sqrt().getNDArrayInternal().rdiv(1.));
    }

    /** {@inheritDoc} */
    @Override
    public NDArray mean() {
        return NDArrays.where(nu.gt(1.0), mu, mu.getManager().full(mu.getShape(), Float.NaN));
    }

    /**
     * Creates a builder to build a {@code NegativeBinomial}.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /** The builder to construct a {@code NegativeBinomial}. */
    public static final class Builder extends DistributionBuilder<Builder> {

        /** {@inheritDoc} */
        @Override
        public Distribution build() {
            Preconditions.checkArgument(
                    distrArgs.contains("mu"), "StudentTl's args must contain mu.");
            Preconditions.checkArgument(
                    distrArgs.contains("sigma"), "StudentTl's args must contain sigma.");
            Preconditions.checkArgument(
                    distrArgs.contains("nu"), "StudentTl's args must contain nu.");
            return new StudentT(this);
        }

        /** {@inheritDoc} */
        @Override
        protected Builder self() {
            return this;
        }
    }
}
