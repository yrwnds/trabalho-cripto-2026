package br.ufsm.poli.csi.tapw.certif;

public class Requisicao implements java.io.Serializable {

    private TipoRequisicao tipoRequisicao;
    private byte[] requisicao;
    private byte[] resposta;
    private byte[] chaveSessao;

    public enum TipoRequisicao {
        ASSINAR_CERTIFICADO, OBTER_CHAVE_PUBLICA, ENVIAR_MENSAGEM
    }

    public byte[] getChaveSessao() {
        return chaveSessao;
    }

    public void setChaveSessao(byte[] chaveSessao) {
        this.chaveSessao = chaveSessao;
    }

    public TipoRequisicao getTipoRequisicao() {
        return tipoRequisicao;
    }

    public void setTipoRequisicao(TipoRequisicao tipoRequisicao) {
        this.tipoRequisicao = tipoRequisicao;
    }

    public byte[] getRequisicao() {
        return requisicao;
    }

    public void setRequisicao(byte[] requisicao) {
        this.requisicao = requisicao;
    }

    public byte[] getResposta() {
        return resposta;
    }

    public void setResposta(byte[] resposta) {
        this.resposta = resposta;
    }
}
