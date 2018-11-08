package me.rockywu.common.monitor;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;



/**
 * @author RockyWu
 * @date 2018/11/8
 */
@Aspect
@Component
public class PrometheusMetricAspect {

    private static final Counter requestTotal = Counter.build().name("counter_total")
            .labelNames("api", "service").help("total request counter for api").register();
    private static final Counter requestSuccess = Counter.build().name("counter_success")
            .labelNames("api", "service").help("total success request counter for api").register();
    private static final Counter requestError = Counter.build().name("counter_error")
            .labelNames("api", "service").help("total error request counter for api").register();
    private static final Histogram histogram = Histogram.build().name("requests_latency_histogram_seconds")
            .labelNames("api", "service").help("Request latency in seconds.").register();

    @Pointcut("@annotation(me.rockywu.common.monitor.PrometheusMetrics)")
    public void pointCutMethod() {
    }

    @Around(value = "pointCutMethod() && @annotation(annotation)")
    public Object collector(ProceedingJoinPoint joinPoint, PrometheusMetrics annotation) throws Throwable {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        PrometheusMetrics prometheusMetrics = methodSignature.getMethod().getAnnotation(PrometheusMetrics.class);

        if (prometheusMetrics != null) {
            String service = prometheusMetrics.service();
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            String api = request.getRequestURI();

            requestTotal.labels(api, service).inc();
            Histogram.Timer requestTimer = histogram.labels(api, service).startTimer();

            Object object;
            try {
                object = joinPoint.proceed();
                requestSuccess.labels(api, service).inc();
            } catch (Throwable throwable) {
                requestError.labels(api, service).inc();
                throw throwable;
            } finally {
                requestTimer.observeDuration();
            }
            return object;

        } else {
            return joinPoint.proceed();
        }
    }


}

