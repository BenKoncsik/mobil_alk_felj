package hu.koncsik.adapter;

import java.io.Serializable;
import java.util.Date;

public class Message_1 implements Serializable {
        private String text;
        private Date send;
        private String sender;

        public Message_1(String text, String sender){
            this.text = text;
            this.sender = sender;
            this.send = new Date();
        }
        public Message_1(){}
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Date getSend() {
            return send;
        }

        public void setSend(Date send) {
            this.send = send;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }
}
