package br.com.ergoperitus

import br.com.ergoperitus.dados.Atividade
import br.com.ergoperitus.dados.ChecklistOcra
import br.com.ergoperitus.dados.ComplementaresOcra
import br.com.ergoperitus.dados.ForcaOcra
import br.com.ergoperitus.dados.PosturaOcra
import java.math.BigDecimal
import java.math.RoundingMode

object CalculadoraOcra {

    data class Resultado(
        val recuperacao: Double?,
        val frequencia: Double?,
        val forca: Double?,
        val postura: Double?,
        val complementares: Double?,
        val duracaoMinutos: Int?,
        val multiplicador: Double?,
        val pontuacaoIntrinseca: Double?,
        val pontuacaoPonderada: Double?,
        val classificacao: String?,
        val pendencias: List<String>,
        val observacoes: List<String>
    )

    fun calcular(
        atividade: Atividade,
        checklist: ChecklistOcra?,
        forca: ForcaOcra?,
        postura: PosturaOcra?,
        complementares: ComplementaresOcra?,
        lado: String
    ): Resultado {
        require(lado == "D" || lado == "E")

        val pendencias = mutableListOf<String>()
        val observacoes = mutableListOf<String>()

        val recuperacao = checklist?.recuperacao?.toDouble()

        if (recuperacao == null) {
            pendencias.add("Selecionar pausas e recuperação.")
        }

        val jornada = checklist?.jornadaMinutos

        if (jornada == null) {
            pendencias.add("Informar a duração efetiva do turno.")
        }

        if (checklist?.distribuicaoPausas.isNullOrBlank()) {
            pendencias.add(
                "Descrever a distribuição das pausas, " +
                        "inclusive quando não houver."
            )
        }

        val frequencia = calcularFrequencia(
            checklist,
            lado,
            pendencias
        )

        val pontosForca = calcularForca(
            forca,
            pendencias,
            observacoes
        )

        val pontosPostura = calcularPostura(
            postura,
            pendencias
        )

        val pontosComplementares = calcularComplementares(
            complementares,
            pendencias
        )

        val duracao = atividade.duracaoDiariaMinutos

        val multiplicador = when {
            duracao == null -> {
                pendencias.add(
                    "Informar a duração diária da atividade."
                )
                null
            }

            duracao !in 1..1440 -> {
                pendencias.add("Revisar a duração diária da atividade.")
                null
            }

            duracao < 60 -> {
                pendencias.add(
                    "A duração é inferior a 60 minutos. " +
                            "O PDF de referência não informa " +
                            "multiplicador para esse intervalo."
                )
                null
            }

            else -> multiplicadorDuracao(duracao)
        }

        if (
            duracao != null &&
            jornada != null &&
            duracao > jornada
        ) {
            pendencias.add(
                "A duração da atividade ultrapassa a duração " +
                        "efetiva do turno. Revise os tempos."
            )
        }

        val blocos = listOf(
            recuperacao,
            frequencia,
            pontosForca,
            pontosPostura,
            pontosComplementares
        )

        val intrinseca = if (blocos.all { it != null }) {
            blocos.filterNotNull().sum()
        } else {
            null
        }

        val ponderada = if (
            pendencias.isEmpty() &&
            intrinseca != null &&
            multiplicador != null
        ) {
            BigDecimal.valueOf(intrinseca)
                .multiply(BigDecimal.valueOf(multiplicador))
                .toDouble()
        } else {
            null
        }

        val classificacao = ponderada?.let {
            classificar(it)
        }

        if (
            ponderada != null &&
            arredondarUmaCasa(ponderada) == 22.6
        ) {
            observacoes.add(
                "O valor arredondado é 22,6. A tabela enviada " +
                        "termina o risco médio em 22,5 e indica " +
                        "risco elevado acima de 22,6. " +
                        "A classificação desse limite requer " +
                        "conferência da referência metodológica."
            )
        }

        return Resultado(
            recuperacao = recuperacao,
            frequencia = frequencia,
            forca = pontosForca,
            postura = pontosPostura,
            complementares = pontosComplementares,
            duracaoMinutos = duracao,
            multiplicador = multiplicador,
            pontuacaoIntrinseca = intrinseca,
            pontuacaoPonderada = ponderada,
            classificacao = classificacao,
            pendencias = pendencias.toList(),
            observacoes = observacoes.toList()
        )
    }

    private fun calcularFrequencia(
        checklist: ChecklistOcra?,
        lado: String,
        pendencias: MutableList<String>
    ): Double? {
        val dinamica = if (lado == "D") {
            checklist?.frequenciaDinamicaDireita
        } else {
            checklist?.frequenciaDinamicaEsquerda
        }

        val estatica = if (lado == "D") {
            checklist?.frequenciaEstaticaDireita
        } else {
            checklist?.frequenciaEstaticaEsquerda
        }

        val concluida = if (lado == "D") {
            checklist?.medicaoDireitaConcluida == true &&
                    checklist.tempoDireitaMs == 60_000L
        } else {
            checklist?.medicaoEsquerdaConcluida == true &&
                    checklist.tempoEsquerdaMs == 60_000L
        }

        if (!concluida) {
            pendencias.add(
                "Concluir a contagem de 60 segundos deste braço."
            )
        }

        if (dinamica == null || estatica == null) {
            pendencias.add(
                "Responder às condições dinâmica e estática " +
                        "da frequência deste braço."
            )
            return null
        }

        return maxOf(dinamica, estatica)
    }

    private fun calcularForca(
        registro: ForcaOcra?,
        pendencias: MutableList<String>,
        observacoes: MutableList<String>
    ): Double? {
        if (registro?.haForcaRepetida == null) {
            pendencias.add("Avaliar a presença de força repetida.")
            return null
        }

        if (!registro.haForcaRepetida) {
            return 0.0
        }

        val intensidades = listOf(
            registro.borgModerada,
            registro.borgForte,
            registro.borgQuaseMaxima
        )

        val tempos = listOf(
            registro.tempoModerada,
            registro.tempoForte,
            registro.tempoQuaseMaxima
        )

        val tabelas = listOf(
            mapOf(
                "TERCO" to 2,
                "METADE" to 4,
                "MAIS_METADE" to 6,
                "QUASE_TODO" to 8
            ),
            mapOf(
                "DOIS_SEGUNDOS" to 4,
                "UM_PORCENTO" to 8,
                "CINCO_PORCENTO" to 16,
                "MAIS_DEZ_PORCENTO" to 24
            ),
            mapOf(
                "DOIS_SEGUNDOS" to 6,
                "UM_PORCENTO" to 12,
                "CINCO_PORCENTO" to 24,
                "MAIS_DEZ_PORCENTO" to 32
            )
        )

        val presentes = intensidades.indices.filter {
            intensidades[it] != null
        }

        if (presentes.isEmpty()) {
            pendencias.add(
                "Registrar intensidade e tempo da força observada."
            )
            return null
        }

        var total = 0

        for (indice in presentes) {
            val pontos = tabelas[indice][tempos[indice]]

            if (pontos == null) {
                pendencias.add(
                    "Informar o tempo de cada intensidade " +
                            "de força registrada."
                )
                return null
            }

            total += pontos
        }

        if (
            registro.tempoForte == "MAIS_DEZ_PORCENTO" ||
            registro.tempoQuaseMaxima == "MAIS_DEZ_PORCENTO"
        ) {
            observacoes.add(
                "Há força forte ou quase máxima por mais de 10% " +
                        "do tempo. O checklist identifica essas " +
                        "condições como não aceitáveis, " +
                        "independentemente da faixa numérica final."
            )
        }

        return total.toDouble()
    }

    private fun calcularPostura(
        registro: PosturaOcra?,
        pendencias: MutableList<String>
    ): Double? {
        if (registro == null) {
            pendencias.add("Preencher posturas e estereotipia.")
            return null
        }

        val ombro = registro.ombro
        val cotovelo = registro.cotovelo
        val punho = registro.punho
        val mao = registro.maoDedos
        val preensoes = registro.preensoes
        val estereotipia = registro.estereotipia
        val acima = registro.maosAcimaCabeca

        if (
            ombro == null ||
            cotovelo == null ||
            punho == null ||
            mao == null ||
            preensoes == null ||
            estereotipia == null ||
            acima == null
        ) {
            pendencias.add(
                "Completar as quatro regiões, tipos de preensão, " +
                        "condição acima da cabeça e estereotipia."
            )
            return null
        }

        if (acima && ombro == 0) {
            pendencias.add(
                "Revisar ombro: trabalho acima da cabeça " +
                        "com pontuação de ombro igual a zero."
            )
            return null
        }

        if (mao > 0 && preensoes == 0) {
            pendencias.add(
                "Selecionar o tipo de preensão correspondente " +
                        "à pontuação de mão e dedos."
            )
            return null
        }

        if (mao == 0 && preensoes > 0) {
            pendencias.add(
                "Revisar a duração das preensões selecionadas " +
                        "e a pontuação de mão e dedos."
            )
            return null
        }

        val ombroAjustado = if (acima) ombro * 2 else ombro

        return maxOf(
            ombroAjustado,
            cotovelo,
            punho,
            mao
        ) + estereotipia
    }

    private fun calcularComplementares(
        registro: ComplementaresOcra?,
        pendencias: MutableList<String>
    ): Double? {
        val fatores = registro?.fatores
        val fisica = registro?.pontuacaoFisica
        val ritmo = registro?.ritmoMaquina

        if (fatores == null || fisica == null || ritmo == null) {
            pendencias.add(
                "Completar fatores complementares, " +
                        "pontuação física e ritmo da máquina."
            )
            return null
        }

        if (
            (fatores == 0 && fisica != 0) ||
            (fatores != 0 && fisica == 0)
        ) {
            pendencias.add(
                "Conferir a correspondência entre os fatores " +
                        "observados e a pontuação física."
            )
            return null
        }

        if (
            fisica == 4 &&
            fatores and ComplementaresOcra.VIBRACAO == 0
        ) {
            pendencias.add(
                "Conferir a condição de elevada vibração " +
                        "para os 4 pontos do bloco físico."
            )
            return null
        }

        if (
            fatores and ComplementaresOcra.OUTROS != 0 &&
            registro.observacoes.isBlank()
        ) {
            pendencias.add(
                "Descrever os outros fatores complementares " +
                        "e sua duração."
            )
            return null
        }

        return (fisica + ritmo).toDouble()
    }

    fun multiplicadorDuracao(minutos: Int): Double? {
        return when (minutos) {
            in 60..120 -> 0.5
            in 121..180 -> 0.65
            in 181..240 -> 0.75
            in 241..300 -> 0.85
            in 301..360 -> 0.925
            in 361..420 -> 0.95
            in 421..480 -> 1.0
            in 481..1440 -> 1.5
            else -> null
        }
    }

    fun arredondarUmaCasa(valor: Double): Double {
        return BigDecimal.valueOf(valor)
            .setScale(1, RoundingMode.HALF_UP)
            .toDouble()
    }

    private fun classificar(valor: Double): String {
        // A classificação usa uma casa decimal, como a tabela.
        val pontos = arredondarUmaCasa(valor)

        return when {
            pontos <= 7.5 ->
                "Faixa verde — risco aceitável"

            pontos <= 11.0 ->
                "Faixa amarela — borderline ou risco muito leve"

            pontos <= 14.0 ->
                "Faixa vermelha leve — risco leve"

            pontos <= 22.5 ->
                "Faixa vermelha média — risco médio"

            pontos == 22.6 ->
                "Limite sem classificação explícita no PDF enviado"

            else ->
                "Faixa violeta — risco elevado"
        }
    }
}