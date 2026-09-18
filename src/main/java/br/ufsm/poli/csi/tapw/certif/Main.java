package br.ufsm.poli.csi.tapw.certif;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

import static br.ufsm.poli.csi.tapw.certif.Requisicao.TipoRequisicao.*;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidKeySpecException {

        // abre conexao com professor
        Socket socket = new Socket("0.0.0.0", 8080);
        System.out.println("Abriu conexão com êxito");
        ObjectOutputStream oout = new ObjectOutputStream(socket.getOutputStream());


        // gera nova requisicao obter chave publica

        Requisicao req = new Requisicao();
        req.setTipoRequisicao(OBTER_CHAVE_PUBLICA);
        oout.writeObject(req);
        oout.flush();
        System.out.println("Enviou req obter chave publica");

        ObjectInputStream oin = new ObjectInputStream(socket.getInputStream());
        // recebe a chave publica do CA

        Requisicao resp = (Requisicao) oin.readObject();
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(resp.getResposta());
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey chavePublica = keyFactory.generatePublic(pubKeySpec);
        System.out.println("Recebeu chave pública do CA");


        //cria a chave de sessao com a chave publica recebida do CA

        KeyGenerator aesKeyGen = KeyGenerator.getInstance("AES");
        aesKeyGen.init(256);
        SecretKey chaveSessao = aesKeyGen.generateKey();

        //crio o cipher e criptografo a chave de sessao
        Cipher rsa = Cipher.getInstance("RSA");
        rsa.init(Cipher.ENCRYPT_MODE, chavePublica);
        byte[] chaveSessaoCifrada = rsa.doFinal(chaveSessao.getEncoded());

        // cria novo certificado

        Certificado cert = new Certificado();
        cert.setNome("Letícia Zanini Nunes");
        cert.setIpOrigem(String.valueOf(InetAddress.getLocalHost().getHostAddress()));
        cert.setChavePublica(resp.getResposta());

        Date validadeInicio = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(validadeInicio);
        calendar.add(Calendar.YEAR, 1);
        Date validadeFim = calendar.getTime();

        cert.setValidadeInicio(validadeInicio);
        cert.setValidadeFim(validadeFim);

        System.out.println("Criou certificado");

        // certificado vira JSON

        ObjectMapper objectMapper = new ObjectMapper();
        String certificadoJSON = objectMapper.writeValueAsString(cert);
        byte[] certificadoBarr = certificadoJSON.getBytes();

        System.out.println(certificadoJSON);

        // criptografa certificado com chave de sessao
        Cipher aes = Cipher.getInstance("AES");
        aes.init(Cipher.ENCRYPT_MODE, chaveSessao);
        byte[] arquivoCifrado = aes.doFinal(certificadoBarr);

        System.out.println("Criptografou certificado");


        // envia requisicao tipo assinar_certificado

        Requisicao req2 = new Requisicao();
        req2.setTipoRequisicao(ASSINAR_CERTIFICADO);
        req2.setChaveSessao(chaveSessaoCifrada);
        req2.setRequisicao(arquivoCifrado);

        System.out.println("Enviou requisição de assinatura de certificado");

        socket.close();
        socket = new Socket("0.0.0.0", 8080);
        oout = new ObjectOutputStream(socket.getOutputStream());
        oout.writeObject(req2);
        oout.flush();

        // recebe certificado assinado
        oin = new ObjectInputStream(socket.getInputStream());
        Requisicao resp2 = (Requisicao) oin.readObject();

        // descriptografa o certificado assinado

        aes.init(Cipher.DECRYPT_MODE, chaveSessao);
        byte[] certDescriptografado = aes.doFinal(resp2.getResposta());

        String certAssinadoJSON = new String(certDescriptografado, StandardCharsets.UTF_8);

        System.out.println(certAssinadoJSON);
        Certificado certAssinado = objectMapper.readValue(certAssinadoJSON, Certificado.class);

        // gera mensagem com certificado assinado

        Mensagem msg = new Mensagem();
        msg.setMensagem("Mensagem teste".getBytes());
        msg.setCertificado(certAssinado);
        msg.setAssinatura(certAssinado.getAssinatura());

        // transforma msg em JSON e depois em bytearray

        String msgJSON = objectMapper.writeValueAsString(msg);
        System.out.println(msgJSON);
        byte[] msgBarr = msgJSON.getBytes();

        // criptografa msg  com chave de sessao
        aes.init(Cipher.ENCRYPT_MODE, chaveSessao);
        byte[] msgCifrada = aes.doFinal(msgBarr);

        // envia requisicao de enviar mensagem

        Requisicao req3 = new Requisicao();
        req3.setTipoRequisicao(ENVIAR_MENSAGEM);
        req3.setRequisicao(msgCifrada);
        req3.setChaveSessao(chaveSessaoCifrada);

        // envia mensagem
        socket.close();
        socket = new Socket("0.0.0.0", 8080);
        oout = new ObjectOutputStream(socket.getOutputStream());
        oout.writeObject(req3);
        oout.flush();
    }
}
