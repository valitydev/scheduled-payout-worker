package dev.vality.scheduledpayoutworker.servlet;

import dev.vality.damsel.claim_management.ClaimCommitterSrv;
import dev.vality.woody.thrift.impl.http.THServiceBuilder;
import lombok.RequiredArgsConstructor;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import java.io.IOException;

@WebServlet("/claim-committer")
@RequiredArgsConstructor
public class ClaimCommitterServlet extends GenericServlet {

    private Servlet thriftServlet;

    private final ClaimCommitterSrv.Iface claimCommitterService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        thriftServlet = new THServiceBuilder()
                .build(ClaimCommitterSrv.Iface.class, claimCommitterService);
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        thriftServlet.service(req, res);
    }

}
