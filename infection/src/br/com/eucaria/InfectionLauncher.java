package br.com.eucaria;

import br.com.eucaria.model.StatusEnum;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class InfectionLauncher {

    public static void main(String[] args) {
        // Inicializa a instância do runtime do JADE.
        Runtime runtime = Runtime.instance();

        // Cria um perfil para configurar o contêiner principal.
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        // Habilita a interface gráfica do JADE para monitoramento.
        profile.setParameter(Profile.GUI, "true");

        // Cria o contêiner principal do sistema multiagente.
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        try {
            // Cria os agentes micróbios iniciais, passando a posição e cor como argumentos
            createMicrobeAgent(mainContainer, "BlueMicrobe-1", 0, 0, StatusEnum.BLUE);
            createMicrobeAgent(mainContainer, "BlueMicrobe-2", 6, 6, StatusEnum.BLUE);
            createMicrobeAgent(mainContainer, "RedMicrobe-1", 6, 0, StatusEnum.RED);
            createMicrobeAgent(mainContainer, "RedMicrobe-2", 0, 6, StatusEnum.RED);

            // Inicia o agente "juiz" para monitorar e encerrar o jogo
            AgentController manager = mainContainer.createNewAgent("SimulationManager", "br.com.eucaria.agent.SimulationManagerAgent", null);
            manager.start();

        } catch (StaleProxyException e) {
            System.err.println("Erro ao iniciar os agentes:");
            e.printStackTrace();
        }
    }

    /**
     * Método auxiliar para criar um novo MicrobeAgent.
     * @param container O contêiner onde o agente será criado.
     * @param agentName O nome do agente.
     * @param x Posição inicial X.
     * @param y Posição inicial Y.
     * @param color A cor/time do micróbio.
     * @throws StaleProxyException
     */
    private static void createMicrobeAgent(AgentContainer container, String agentName, int x, int y, StatusEnum color) throws StaleProxyException {
        // Os argumentos são passados como um array de Object
        Object[] args = {x, y, color};
        AgentController agentController = container.createNewAgent(agentName, "br.com.eucaria.agent.MicrobeAgent", args);
        agentController.start();
    }
}