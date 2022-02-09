package dev.vality.scheduledpayoutworker.servlet;

import dev.vality.damsel.schedule.ScheduledJobExecutorSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import lombok.RequiredArgsConstructor;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet("/v1/schedulator")
@RequiredArgsConstructor
public class SchedulatorServlet extends GenericServlet {

    private final ScheduledJobExecutorSrv.Iface requestHandler;
    private Servlet thriftServlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(ScheduledJobExecutorSrv.Iface.class, requestHandler);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }
}
