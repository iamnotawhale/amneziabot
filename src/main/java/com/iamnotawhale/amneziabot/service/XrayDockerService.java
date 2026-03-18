package com.iamnotawhale.amneziabot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iamnotawhale.amneziabot.config.XrayProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class XrayDockerService {

    private final CommandExecutor commandExecutor;
    private final ObjectMapper objectMapper;
    private final XrayProperties xrayProperties;

    public XrayDockerService(CommandExecutor commandExecutor, ObjectMapper objectMapper, XrayProperties xrayProperties) {
        this.commandExecutor = commandExecutor;
        this.objectMapper = objectMapper;
        this.xrayProperties = xrayProperties;
    }

    @Transactional
    public void addClient(String uuid, String email) {
        ObjectNode root = readConfig();
        ArrayNode clients = clientsArray(root);

        boolean exists = false;
        for (JsonNode client : clients) {
            if (uuid.equals(client.path("id").asText())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            ObjectNode clientNode = objectMapper.createObjectNode();
            clientNode.put("id", uuid);
            clientNode.put("flow", xrayProperties.getFlow());
            clientNode.put("email", email);
            clients.add(clientNode);
            writeValidateRestart(root);
        }
    }

    @Transactional
    public void removeClient(String uuid) {
        ObjectNode root = readConfig();
        ArrayNode clients = clientsArray(root);
        for (int i = clients.size() - 1; i >= 0; i--) {
            JsonNode client = clients.get(i);
            if (uuid.equals(client.path("id").asText())) {
                clients.remove(i);
            }
        }
        writeValidateRestart(root);
    }

    private ObjectNode readConfig() {
        try {
            String config = commandExecutor.run(List.of(
                    "docker", "exec", xrayProperties.getContainerName(), "sh", "-lc",
                    "cat " + xrayProperties.getConfigPath()
            ));
            return (ObjectNode) objectMapper.readTree(config);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to read Xray config", e);
        }
    }

    private ArrayNode clientsArray(ObjectNode root) {
        JsonNode inboundsNode = root.path("inbounds");
        if (!inboundsNode.isArray() || inboundsNode.isEmpty()) {
            throw new IllegalStateException("inbounds section missing in Xray config");
        }
        JsonNode firstInbound = inboundsNode.get(0);
        JsonNode clientsNode = firstInbound.path("settings").path("clients");
        if (!clientsNode.isArray()) {
            throw new IllegalStateException("settings.clients section missing in Xray config");
        }
        return (ArrayNode) clientsNode;
    }

    private void writeValidateRestart(ObjectNode root) {
        try {
            String updated = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            commandExecutor.run(List.of(
                    "docker", "exec", "-i", xrayProperties.getContainerName(), "sh", "-lc",
                    "cat > " + xrayProperties.getConfigPath()
            ), updated);

            commandExecutor.run(List.of(
                    "docker", "exec", xrayProperties.getContainerName(), "sh", "-lc",
                    "xray -test -config " + xrayProperties.getConfigPath()
            ));

            commandExecutor.run(List.of("docker", "restart", xrayProperties.getContainerName()));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to apply Xray config", e);
        }
    }
}
