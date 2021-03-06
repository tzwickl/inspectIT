package rocks.inspectit.ui.rcp.editor.graph.plot.datasolver.impl;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.RangeType;

import rocks.inspectit.ui.rcp.editor.graph.plot.datasolver.AbstractPlotDataSolver;

/**
 * Default implementation of the {@link AbstractPlotDataSolver}. This implementation will be used by
 * default.
 *
 * @author Marius Oehler
 *
 */
public class DefaultDataSolver extends AbstractPlotDataSolver {

	/**
	 * Package-private Constructor.
	 */
	DefaultDataSolver() {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NumberAxis getAxis() {
		NumberAxis rangeAxis = new NumberAxis("");
		rangeAxis.setAutoRangeIncludesZero(false);
		rangeAxis.setRangeType(RangeType.POSITIVE);
		rangeAxis.setAutoRange(true);
		return rangeAxis;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String valueToHumanReadableImpl(double value) {
		return String.valueOf(value);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isAggregatable() {
		return true;
	}
}