package rocks.inspectit.agent.java.sensor.platform;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;

import io.opencensus.stats.Aggregation;
import io.opencensus.stats.Measure.MeasureDouble;
import io.opencensus.stats.Measure.MeasureLong;
import io.opencensus.stats.MeasureMap;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.View;
import io.opencensus.stats.ViewManager;
import io.opencensus.tags.TagKey;
import rocks.inspectit.agent.java.sensor.platform.provider.OperatingSystemInfoProvider;
import rocks.inspectit.agent.java.sensor.platform.provider.factory.PlatformSensorInfoProviderFactory;
import rocks.inspectit.shared.all.communication.SystemSensorData;
import rocks.inspectit.shared.all.communication.data.CpuInformationData;

/**
 * This class provides dynamic information about the underlying operating system through MXBeans.
 *
 * @author Eduard Tudenhoefner
 * @author Max Wassiljew (NovaTec Consulting GmbH)
 */
public class CpuInformation extends AbstractPlatformSensor {

	/** Collector class. */
	private CpuInformationData cpuInformationData = new CpuInformationData();

	private static final StatsRecorder statsRecorder = Stats.getStatsRecorder();
	private static final ViewManager viewManager = Stats.getViewManager();

	private static final TagKey CPU = TagKey.create("cpu");

	private static final MeasureDouble CPU_USAGE = MeasureDouble.create("cpu_usage", "CPU Usage", "Usage");
	private static final MeasureDouble CPU_USAGE_MIN = MeasureDouble.create("cpu_usage_min", "CPU Usage Min", "Usage");
	private static final MeasureDouble CPU_USAGE_MAX = MeasureDouble.create("cpu_usage_max", "CPU Usage MAx", "Usage");
	private static final MeasureLong CPU_TIME = MeasureLong.create("cpu_time", "CPU Time", "ms");

	private static final View.Name CPU_USAGE_VIEW_NAME = View.Name.create("cpu_usage");
	private static final View CPU_USAGE_VIEW = View.create(CPU_USAGE_VIEW_NAME, "CPU Usage", CPU_USAGE, Aggregation.Sum.create(), Collections.singletonList(CPU));

	private static final View.Name CPU_USAGE_MAX_VIEW_NAME = View.Name.create("cpu_usage_max");
	private static final View CPU_USAGE_MAX_VIEW = View.create(CPU_USAGE_MAX_VIEW_NAME, "Maximum CPU Usage", CPU_USAGE_MAX, Aggregation.LastValue.create(), Collections.singletonList(CPU));

	private static final View.Name CPU_USAGE_MIN_VIEW_NAME = View.Name.create("cpu_usage_min");
	private static final View CPU_USAGE_MIN_VIEW = View.create(CPU_USAGE_MIN_VIEW_NAME, "Minimum CPU Usage", CPU_USAGE_MIN, Aggregation.LastValue.create(), Collections.singletonList(CPU));

	private static final View.Name CPU_TIME_VIEW_NAME = View.Name.create("cpu_time");
	private static final View CPU_TIME_VIEW = View.create(CPU_TIME_VIEW_NAME, "CPU Time", CPU_TIME, Aggregation.LastValue.create(), Collections.singletonList(CPU));

	static {
		viewManager.registerView(CPU_USAGE_VIEW);
		viewManager.registerView(CPU_USAGE_MIN_VIEW);
		viewManager.registerView(CPU_USAGE_MAX_VIEW);
		viewManager.registerView(CPU_TIME_VIEW);
	}

	/**
	 * The {@link OperatingSystemInfoProvider} used to retrieve information from the operating
	 * system.
	 */
	private OperatingSystemInfoProvider osBean;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void gather() {
		// The timestamp is set in the {@link CpuInformation#reset()} to avoid multiple renewal. It
		// will not be set on the first execution of {@link CpuInformation#gather()}, but shortly
		// before.
		float cpuUsage = this.getOsBean().retrieveCpuUsage();
		long cpuTime = this.getOsBean().getProcessCpuTime();

		MeasureMap measureMap = statsRecorder.newMeasureMap();

		measureMap.put(CPU_USAGE, cpuUsage);
		measureMap.put(CPU_TIME, cpuTime);

		this.cpuInformationData.incrementCount();
		this.cpuInformationData.updateProcessCpuTime(cpuTime);
		this.cpuInformationData.addCpuUsage(cpuUsage);

		if (cpuUsage < this.cpuInformationData.getMinCpuUsage()) {
			this.cpuInformationData.setMinCpuUsage(cpuUsage);
			measureMap.put(CPU_USAGE_MIN, cpuUsage);
		}
		if (cpuUsage > this.cpuInformationData.getMaxCpuUsage()) {
			this.cpuInformationData.setMaxCpuUsage(cpuUsage);
			measureMap.put(CPU_USAGE_MAX, cpuUsage);
		}

		measureMap.record();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SystemSensorData get() {
		CpuInformationData newCpuInformationData = new CpuInformationData();

		newCpuInformationData.setPlatformIdent(this.cpuInformationData.getPlatformIdent());
		newCpuInformationData.setSensorTypeIdent(this.cpuInformationData.getSensorTypeIdent());
		newCpuInformationData.setCount(this.cpuInformationData.getCount());

		newCpuInformationData.setProcessCpuTime(this.cpuInformationData.getProcessCpuTime());

		newCpuInformationData.setTotalCpuUsage(this.cpuInformationData.getTotalCpuUsage());
		newCpuInformationData.setMinCpuUsage(this.cpuInformationData.getMinCpuUsage());
		newCpuInformationData.setMaxCpuUsage(this.cpuInformationData.getMaxCpuUsage());

		newCpuInformationData.setTimeStamp(this.cpuInformationData.getTimeStamp());

		return newCpuInformationData;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void reset() {
		this.cpuInformationData.setCount(0);

		this.cpuInformationData.setProcessCpuTime(0L);

		this.cpuInformationData.setTotalCpuUsage(0f);
		this.cpuInformationData.setMinCpuUsage(Float.MAX_VALUE);
		this.cpuInformationData.setMaxCpuUsage(0f);

		Timestamp timestamp = new Timestamp(Calendar.getInstance().getTimeInMillis());
		this.cpuInformationData.setTimeStamp(timestamp);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected SystemSensorData getSystemSensorData() {
		return this.cpuInformationData;
	}

	/**
	 * Gets the {@link OperatingSystemInfoProvider}. The getter method is provided for better
	 * testability.
	 *
	 * @return {@link OperatingSystemInfoProvider}.
	 */
	private OperatingSystemInfoProvider getOsBean() {
		if (this.osBean == null) {
			this.osBean = PlatformSensorInfoProviderFactory.getPlatformSensorInfoProvider().getOperatingSystemInfoProvider();
		}
		return this.osBean;
	}
}
