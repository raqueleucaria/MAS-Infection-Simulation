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

public class Main {

    private static final System.Logger LOGGER = System.getLogger(Main.class.getName());
    private static final int NUMBER_OF_SIMULATIONS = 5;
    private static final boolean SHOW_GUI = false;

    public static void main(String[] args) {

        System.out.println("***************************************************");
        System.out.println("* I N F E C T I O N - S I M U L A T I O N     *");
        System.out.println("***************************************************");

        for (int i = 1; i <= NUMBER_OF_SIMULATIONS; i++) {
            System.out.println("\n===================================================");
            LOGGER.log(System.Logger.Level.INFO, "Iniciando ambiente para a simulação de número: {0}", i);
            System.out.println("===================================================");

            Runtime runtime = Runtime.instance();
            runtime.setCloseVM(true);

            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");

            profile.setParameter(Profile.LOCAL_PORT, String.valueOf(1100 + i));

            profile.setParameter(Profile.GUI, String.valueOf(SHOW_GUI));

            AgentContainer mainContainer = runtime.createMainContainer(profile);

            try {
                Object[] managerArgs = { i };
                AgentController manager = mainContainer.createNewAgent(
                        "SimulationManager_" + i,
                        "br.com.eucaria.agent.SimulationManagerAgent",
                        managerArgs
                );
                manager.start();

                createMicrobeAgent(mainContainer, UUID.randomUUID().toString(), 0, 0, BLUE);
                createMicrobeAgent(mainContainer, UUID.randomUUID().toString(), 6, 6, BLUE);
                createMicrobeAgent(mainContainer, UUID.randomUUID().toString(), 6, 0, RED);
                createMicrobeAgent(mainContainer, UUID.randomUUID().toString(), 0, 6, RED);

                LOGGER.log(System.Logger.Level.INFO, "Agentes iniciais para a rodada {0} criados com sucesso.", i);

                Thread.sleep(1000);
            } catch (StaleProxyException | InterruptedException e) {
                LOGGER.log(System.Logger.Level.ERROR, "Erro ao iniciar os agentes na rodada " + i, e);
            }

        }

        System.out.println("\n***************************************************");
        System.out.println("* TODAS AS SIMULAÇÕES FORAM CONCLUÍDAS     *");
        System.out.println("***************************************************");
    }

    private static void createMicrobeAgent(
            AgentContainer container,
            String agentName,
            int x,
            int y,
            MicrobeColorEnum color
    ) throws StaleProxyException {

        Object[] args = {x, y, color};
        AgentController agentController = container.createNewAgent(
                agentName, "br.com.eucaria.agent.MicrobeAgent", args
        );
        agentController.start();
    }
}