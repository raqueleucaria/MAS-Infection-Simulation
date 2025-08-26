# Aplicação e Análise de Algoritmos em Cenários de Uso Comportamentais<br>
**Autora**: Raquel Eucaria

## Sobre o Projeto
O **Infection** é uma simulação de competição biológica desenvolvida para explorar dinâmicas de Sistemas Multiagentes. O projeto modela a disputa por território entre duas colônias de micróbios (Vermelha e Azul) em um ambiente discreto (um tabuleiro 7x7), onde o comportamento coletivo e os padrões de dominação emergem a partir de regras individuais simples.

A simulação foi desenvolvida em **Java** utilizando o framework **JADE**, que gerencia o ciclo de vida e a comunicação assíncrona entre os agentes através do protocolo FIPA ACL.

[//]: # (<div align='center'><center>)

[//]: # (<img src="docs/assets/infection_game.jpg" width="60%">)

[//]: # (<label>Jogo "Infection" de The 7th Guest, inspiração para a dinâmica da simulação.</label>)

[//]: # (</center></div>)

Os agentes implementados na simulação são:

* **`SimulationManagerAgent`**: Atua como o ambiente, gerenciando o estado do tabuleiro, validando movimentos, aplicando as regras de infecção e determinando o fim da partida.
* **`MicrobeAgent`**: Agente reativo que representa um micróbio. A cada ciclo, ele percebe o ambiente, decide sua próxima ação (Copiar ou Pular) com base em uma estratégia para maximizar a infecção, e propõe o movimento ao gerenciador.

[//]: # (Mais detalhes sobre a arquitetura, o comportamento dos agentes e as regras de negócio podem ser encontrados na documentação técnica do projeto.)

[//]: # ()
[//]: # (* **[📄 Documentação de Implementação]&#40;implementacao.md&#41;**)

[//]: # (* **[📐 Documentação de Modelagem &#40;AUML&#41;]&#40;modelagem.md&#41;**)

[//]: # (## Screenshots)

[//]: # ()
[//]: # (<div align='center'>)

[//]: # (  <img src="simulation_animation.gif" /><br><br>)

[//]: # (  <label><strong> Imagem 1:</strong> Execução da Simulação<br> <strong>Fonte:</strong> Autoria Própria, 2025.</label><br><br><br>)

[//]: # (</div>)

## Instalação

1.  **Pré-requisitos:**
    * Java Development Kit (JDK) 11 ou superior.
    * Biblioteca JADE configurada no classpath do seu ambiente de desenvolvimento.

2.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/raqueleucaria/MAS-Infection-Simulation.git](https://github.com/raqueleucaria/MAS-Infection-Simulation.git)
    cd MAS-Infection-Simulation
    ```

## Uso

1.  **Abra o projeto** em sua IDE de preferência (ex: IntelliJ IDEA, Eclipse).

2.  **Configure o classpath** para incluir a biblioteca `jade.jar`.

3.  **Execute a classe principal** para iniciar a simulação:
    ```
    br.com.eucaria.InfectionLauncher
    ```
    A GUI do JADE será iniciada, e a simulação começará automaticamente no console.


## Referências

> [1] WOOLDRIDGE, M. *An introduction to multiagent systems*. 2 ed. Chichester: Wiley, 2009.
>
> [2] WIKIPEDIA. *The 7th Guest: Infection*. Disponível em: <https://en.wikipedia.org/wiki/The_7th_Guest:_Infection>. Acesso em: 24 ago. 2025.
>
> [3] JADE. *JADE (Java Agent DEvelopment Framework)*. Disponível em: https://jade.tilab.com/. Acesso em: 24 ago. 2025.
>
> [4] FIPA. *Foundation for Intelligent Physical Agents*. Disponível em: http://fipa.org/. Acesso em: 24 ago. 2025.