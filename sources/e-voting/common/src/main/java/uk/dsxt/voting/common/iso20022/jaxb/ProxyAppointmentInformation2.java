//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.02.25 at 03:58:45 PM GMT+03:00 
//


package uk.dsxt.voting.common.iso20022.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for ProxyAppointmentInformation2 complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ProxyAppointmentInformation2">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RegnMtd" type="{}Max350Text" minOccurs="0"/>
 *         &lt;element name="Ddln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="STPDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="MktDdln" type="{}DateFormat2Choice" minOccurs="0"/>
 *         &lt;element name="AuthrsdPrxy" type="{}Proxy3" maxOccurs="10" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ProxyAppointmentInformation2", propOrder = {
    "regnMtd",
    "ddln",
    "stpDdln",
    "mktDdln",
    "authrsdPrxy"
})
public class ProxyAppointmentInformation2 {

    @XmlElement(name = "RegnMtd")
    protected String regnMtd;
    @XmlElement(name = "Ddln")
    protected DateFormat2Choice ddln;
    @XmlElement(name = "STPDdln")
    protected DateFormat2Choice stpDdln;
    @XmlElement(name = "MktDdln")
    protected DateFormat2Choice mktDdln;
    @XmlElement(name = "AuthrsdPrxy")
    protected List<Proxy3> authrsdPrxy;

    /**
     * Gets the value of the regnMtd property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRegnMtd() {
        return regnMtd;
    }

    /**
     * Sets the value of the regnMtd property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRegnMtd(String value) {
        this.regnMtd = value;
    }

    /**
     * Gets the value of the ddln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getDdln() {
        return ddln;
    }

    /**
     * Sets the value of the ddln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setDdln(DateFormat2Choice value) {
        this.ddln = value;
    }

    /**
     * Gets the value of the stpDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getSTPDdln() {
        return stpDdln;
    }

    /**
     * Sets the value of the stpDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setSTPDdln(DateFormat2Choice value) {
        this.stpDdln = value;
    }

    /**
     * Gets the value of the mktDdln property.
     * 
     * @return
     *     possible object is
     *     {@link DateFormat2Choice }
     *     
     */
    public DateFormat2Choice getMktDdln() {
        return mktDdln;
    }

    /**
     * Sets the value of the mktDdln property.
     * 
     * @param value
     *     allowed object is
     *     {@link DateFormat2Choice }
     *     
     */
    public void setMktDdln(DateFormat2Choice value) {
        this.mktDdln = value;
    }

    /**
     * Gets the value of the authrsdPrxy property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the authrsdPrxy property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAuthrsdPrxy().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Proxy3 }
     * 
     * 
     */
    public List<Proxy3> getAuthrsdPrxy() {
        if (authrsdPrxy == null) {
            authrsdPrxy = new ArrayList<Proxy3>();
        }
        return this.authrsdPrxy;
    }

}
