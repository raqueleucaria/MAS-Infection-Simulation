package br.com.eucaria;

import br.com.eucaria.model.MicrobeColorEnum;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.UUID;

import static br.com.eucaria.model.MicrobeColorEnum.BLUE;
import static br.com.eucaria.model.MicrobeColorEnum.RED;

public class InfectionLauncher {

    private static final System.Logger LOGGER = System.getLogger(InfectionLauncher.class.getName());

    public static void main(String[] args) {
        Runtime runtime = Runtime.instance();
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");
        AgentContainer mainContainer = runtime.createMainContainer(profile);

        try {
            // Cria o agente que representa o AMBIENTE primeiro
            AgentController manager = mainContainer.createNewAgent(
                    "SimulationManager",
                    "br.com.eucaria.agent.SimulationManagerAgent",
                    null
            );
            manager.start();
            LOGGER.log(System.Logger.Level.INFO, "Ambiente (SimulationManager) iniciado.");

            // Cria os agentes micróbios iniciais
            createMicrobeAgent(mainContainer, UUID.randomUUID().toString(), 0, 0, BLUE);
            createMicrobeAgent(mainContainer, UUID.randomUUID().toString(), 6, 6, BLUE);
            createMicrobeAgent(mainContainer, UUID.randomUUID().toString(), 6, 0, RED);
            createMicrobeAgent(mainContainer, UUID.randomUUID().toString(), 0, 6, RED);

            LOGGER.log(System.Logger.Level.INFO, "Agentes iniciais criados com sucesso.");

        } catch (StaleProxyException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Erro ao iniciar os agentes.", e);
        }
    }

    private static void createMicrobeAgent(
            AgentContainer container,
            String agentName,
            int x,
            int y,
            MicrobeColorEnum color
    ) throws StaleProxyException {
        // Os argumentos (estado inicial) são passados diretamente para o agente
        Object[] args = {x, y, color};
        AgentController agentController = container.createNewAgent(
                agentName, "br.com.eucaria.agent.MicrobeAgent", args
        );
        agentController.start();
    }
}