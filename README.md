
# 🦠 Infection

Uma implementação de Sistemas Multiagentes Comportamentais para o jogo Infection.

## 📜 Regras do Jogo

- 🎯 O jogo tem como objetivo que um tipo de micrópio domine o tabuleiro até o final do jogo.
- 🧩 O tabuleiro é uma matriz 7x7.
- 🔵 Dois micróbios azuis são posicionados nos cantos superior esquerdo (0,0) e inferior direito (6,6).
- 🔴 Dois micróbios vermelhos ocupam os outros dois cantos restantes (0,6), (6,0).
- ♟️ O jogador vermelho começa o primeiro turno se movendo. Os turnos alternam entre vermelho e azul.
 
### 🔁 Movimentação

A cada turno, é possível realizar um movimento com qualquer um de seus micróbios:

- **Copiar**: O micróbio pode se replicar para uma casa adjacente (distância 1). O original permanece no lugar.
- **Pular**: O micróbio pode pular para uma casa a duas posições de distância. Nesse caso, a posição de origem fica vazia.

### 🧬 Infecção

Após o movimento, se houver micróbios oponentes nas casas adjacentes à nova posição, eles são infectados (convertidos) para o lado que se moveu.

### ⏳ Fim de Jogo

- O jogo termina quando todas as células do tabuleiro estiverem preenchidas.
- 🏆 Vence o micróbio com maior quantidade no tabuleiro.

## Referências

- https://en.wikipedia.org/wiki/The_7th_Guest:_Infection
- https://www.youtube.com/watch?v=4_NzTapUqqM
- https://leoribeiro.github.io/papers/mcts-ataxx-eniac2018.pdf