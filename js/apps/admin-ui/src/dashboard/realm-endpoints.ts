export type RealmEndpoint = {
  id: string;
  labelKey: string;
  url: string;
};

function realmBaseUrl(serverBaseUrl: string, realm: string) {
  const base = serverBaseUrl.endsWith("/")
    ? serverBaseUrl
    : `${serverBaseUrl}/`;
  return `${base}realms/${encodeURIComponent(realm)}`;
}

export function buildRealmEndpoints(
  serverBaseUrl: string,
  realm: string,
): RealmEndpoint[] {
  const realmBase = realmBaseUrl(serverBaseUrl, realm);
  return [
    {
      id: "issuer",
      labelKey: "openidIssuer",
      url: realmBase,
    },
    {
      id: "oidc",
      labelKey: "openidConfiguration",
      url: `${realmBase}/.well-known/openid-configuration`,
    },
    {
      id: "cas",
      labelKey: "casDiscoveryEndpoint",
      url: `${realmBase}/protocol/cas/.well-known/cas`,
    },
  ];
}
