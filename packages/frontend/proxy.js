const http = require("http");

const LISTEN_PORT = 3000;
const NEXT_PORT = 3001;
const BACKEND_PORT = 8080;
const SAGEMAKER_PREFIX = "/codeeditor/default";
const BASE_PATH = "/absports/3000";
const FULL_PREFIX = SAGEMAKER_PREFIX + BASE_PATH;

function proxyRequest(req, res, targetPort, targetPath) {
  const options = {
    hostname: "127.0.0.1",
    port: targetPort,
    path: targetPath,
    method: req.method,
    headers: { ...req.headers, host: `127.0.0.1:${targetPort}` },
  };

  const proxyReq = http.request(options, (proxyRes) => {
    let location = proxyRes.headers["location"];
    if (location) {
      if (location.startsWith(BASE_PATH)) {
        location = FULL_PREFIX + location.slice(BASE_PATH.length);
      } else if (location.startsWith("/") && !location.startsWith(SAGEMAKER_PREFIX)) {
        location = FULL_PREFIX + location;
      }
      proxyRes.headers["location"] = location;
    }
    res.writeHead(proxyRes.statusCode, proxyRes.headers);
    proxyRes.pipe(res);
  });

  proxyReq.on("error", (err) => {
    console.error(`Proxy error: ${err.message}`);
    res.writeHead(502);
    res.end("Bad Gateway");
  });

  req.pipe(proxyReq);
}

const server = http.createServer((req, res) => {
  const path = req.url;

  // SageMaker strips /codeeditor/default before forwarding to port 3000.
  // We receive: /absports/3000/...
  //
  // Next.js basePath = /absports/3000, so pass paths starting with BASE_PATH
  // directly to Next.js (it expects them). API requests are stripped and sent to backend.

  if (path.startsWith(BASE_PATH + "/api/") || path === BASE_PATH + "/api") {
    const apiPath = path.slice(BASE_PATH.length);
    proxyRequest(req, res, BACKEND_PORT, apiPath);
  } else if (path.startsWith(BASE_PATH)) {
    proxyRequest(req, res, NEXT_PORT, path);
  } else if (path === "/") {
    res.writeHead(307, { Location: FULL_PREFIX + "/" });
    res.end();
  } else {
    res.writeHead(404);
    res.end("Not Found");
  }
});

server.listen(LISTEN_PORT, "0.0.0.0", () => {
  console.log(`SageMaker proxy listening on :${LISTEN_PORT}`);
  console.log(`  Next.js  -> 127.0.0.1:${NEXT_PORT} (basePath: ${BASE_PATH})`);
  console.log(`  Backend  -> 127.0.0.1:${BACKEND_PORT}`);
  console.log(`  Full prefix: ${FULL_PREFIX}`);
});
