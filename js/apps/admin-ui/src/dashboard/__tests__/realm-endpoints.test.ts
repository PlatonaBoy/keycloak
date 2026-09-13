import { describe, expect, it } from "vitest";
import { buildRealmEndpoints } from "../realm-endpoints";

describe("buildRealmEndpoints", () => {
  it("builds OpenID and CAS discovery URLs for a realm", () => {
    const endpoints = buildRealmEndpoints("https://auth.example.com", "demo");

    expect(endpoints).toEqual([
      {
        id: "issuer",
        labelKey: "openidIssuer",
        url: "https://auth.example.com/realms/demo",
      },
      {
        id: "oidc",
        labelKey: "openidConfiguration",
        url: "https://auth.example.com/realms/demo/.well-known/openid-configuration",
      },
      {
        id: "cas",
        labelKey: "casDiscoveryEndpoint",
        url: "https://auth.example.com/realms/demo/protocol/cas/.well-known/cas",
      },
    ]);
  });

  it("encodes realm names and accepts a trailing slash on the server URL", () => {
    const endpoints = buildRealmEndpoints(
      "https://auth.example.com/",
      "my realm",
    );

    expect(endpoints[0].url).toBe("https://auth.example.com/realms/my%20realm");
  });
});
