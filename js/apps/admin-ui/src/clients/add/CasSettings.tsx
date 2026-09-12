import {
  ClipboardCopy,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  FormGroup,
} from "@patternfly/react-core";
import { useTranslation } from "react-i18next";
import {
  HelpItem,
  SelectControl,
  useEnvironment,
} from "@keycloak/keycloak-ui-shared";
import { DefaultSwitchControl } from "../../components/SwitchControl";
import { FormAccess } from "../../components/form/FormAccess";
import { useRealm } from "../../context/realm-context/RealmContext";
import { addTrailingSlash, convertAttributeNameToForm } from "../../util";
import { FormFields } from "../ClientDetails";
import { PROTOCOL_CAS } from "../constants";

export const CasSettings = () => {
  const { t } = useTranslation();
  const { environment } = useEnvironment();
  const { realm } = useRealm();

  const casBase = `${addTrailingSlash(environment.serverBaseUrl)}realms/${realm}/protocol/${PROTOCOL_CAS}`;

  const endpoints = [
    { id: "login", label: t("casLoginEndpoint"), url: `${casBase}/login` },
    { id: "logout", label: t("casLogoutEndpoint"), url: `${casBase}/logout` },
    {
      id: "validate",
      label: t("casValidateEndpoint"),
      url: `${casBase}/validate`,
    },
    {
      id: "serviceValidate",
      label: t("casServiceValidateEndpoint"),
      url: `${casBase}/serviceValidate`,
    },
    {
      id: "rest",
      label: t("casRestTicketsEndpoint"),
      url: `${casBase}/v1/tickets`,
    },
    {
      id: "discovery",
      label: t("casDiscoveryEndpoint"),
      url: `${casBase}/.well-known/cas`,
    },
  ];

  return (
    <FormAccess
      isHorizontal
      role="manage-clients"
      className="keycloak__capability-config__form"
    >
      <SelectControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.cas.protocol.version",
        )}
        label={t("casProtocolVersion")}
        labelIcon={t("casProtocolVersionHelp")}
        controller={{
          defaultValue: "7.0",
        }}
        options={[
          { key: "1.0", value: t("casVersion10") },
          { key: "2.0", value: t("casVersion20") },
          { key: "3.0", value: t("casVersion30") },
          { key: "7.0", value: t("casVersion70") },
        ]}
      />
      <DefaultSwitchControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.cas.rest.enabled",
        )}
        label={t("casRestEnabled")}
        labelIcon={t("casRestEnabledHelp")}
        stringify
      />
      <DefaultSwitchControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.cas.slo.enabled",
        )}
        label={t("casSloEnabled")}
        labelIcon={t("casSloEnabledHelp")}
        stringify
      />
      <DefaultSwitchControl
        name={convertAttributeNameToForm<FormFields>(
          "attributes.cas.jwt.tickets",
        )}
        label={t("casJwtTickets")}
        labelIcon={t("casJwtTicketsHelp")}
        stringify
      />
      <FormGroup
        label={t("casEndpoints")}
        fieldId="cas-endpoints"
        labelIcon={
          <HelpItem
            helpText={t("casEndpointsHelp")}
            fieldLabelId="casEndpoints"
          />
        }
      >
        <DescriptionList isCompact isHorizontal>
          {endpoints.map((endpoint) => (
            <DescriptionListGroup key={endpoint.id}>
              <DescriptionListTerm>{endpoint.label}</DescriptionListTerm>
              <DescriptionListDescription>
                <ClipboardCopy
                  isReadOnly
                  hoverTip={t("copyToClipboard")}
                  clickTip={t("copySuccess")}
                  data-testid={`cas-endpoint-${endpoint.id}`}
                >
                  {endpoint.url}
                </ClipboardCopy>
              </DescriptionListDescription>
            </DescriptionListGroup>
          ))}
        </DescriptionList>
      </FormGroup>
    </FormAccess>
  );
};
