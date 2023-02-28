package entities;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 *
 * @author ignnov
 */
public class Date extends GregorianCalendar {

    public static final String DEFAULT_PATTERN = "dd/MM/yyyy";
    public static final String WS_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String VFP_PATTERN = "MM-dd-yyyy";
    public static final String DIA_LETRAS = "EEEE";
    public static final String EXP_PREIMP_PATTERN = "d/M/yyyy";
    public static final String FECHA_HORA_PATTERN = "dd/MM/yyyy HH:mm";
    public static final String FECHA_HORA_PATTERN_SQL = "dd/MM/yyyy HH:mm:ss.SSS";
    public static final String FECHA_HORA_PATTERN_JPQL = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String FECHA_HORA_AFIP = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

    public Date() {
        super();
    }

    public Date(java.util.Date date) {
        this.setTime(date);
    }

    public Date(String fecha) throws Exception {
        Pattern pattern = Pattern.compile("(\\d{2})+[-/](\\d{2})+[-/](\\d{4})");
        Matcher matcher = pattern.matcher(fecha);

        if (!matcher.matches()) {
            throw new Exception("Fecha incorrecta");
        }

        int dia = new Integer(matcher.group(1));
        int mes = new Integer(matcher.group(2));
        int anio = new Integer(matcher.group(3));

        this.set(Calendar.YEAR, anio);
        this.set(Calendar.MONTH, mes - 1);
        this.set(Calendar.DAY_OF_MONTH, dia);
    }

    public Date(String fecha, String patron) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(patron);
        this.setTime(sdf.parse(fecha));
    }

    public Date(XMLGregorianCalendar xgc) {
        this.setTime(xgc.toGregorianCalendar().getTime());
    }

    public void diaSiguiente() {
        incrementarDia(1);
    }

    public void incrementarDia(int cantidadDias) {
        this.add(Calendar.DAY_OF_MONTH, cantidadDias);
    }

    public static java.util.Date incrementarXDias(java.util.Date date, int dias) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_YEAR, dias);

        return calendar.getTime();
    }

    public static java.util.Date primerDiaAnio(java.util.Date date) {
        GregorianCalendar gcResultado = new GregorianCalendar();
        gcResultado.setTime(date);
        gcResultado.set(Calendar.DAY_OF_MONTH, 1);
        gcResultado.set(Calendar.MONTH, 0);

        return gcResultado.getTime();
    }

    public static java.util.Date primerDiaAnioMes(int mes, int anio) {
        GregorianCalendar gcResultado = new GregorianCalendar();
        gcResultado.set(Calendar.YEAR, anio);
        gcResultado.set(Calendar.MONTH, mes - 1);
        gcResultado.set(Calendar.DAY_OF_MONTH, 1);

        return gcResultado.getTime();
    }

    public static java.util.Date ultimoDiaAnioMes(int mes, int anio) {
        GregorianCalendar gcResultado = new GregorianCalendar();
        gcResultado.set(Calendar.DAY_OF_MONTH, 1);
        gcResultado.set(Calendar.YEAR, anio);
        gcResultado.set(Calendar.MONTH, mes);

        gcResultado.add(Calendar.DATE, -1);

        return gcResultado.getTime();
    }

    public static java.util.Date ultimoDiaMes(java.util.Date date) {
        GregorianCalendar gcParam = new GregorianCalendar();
        gcParam.setTime(date);

        GregorianCalendar gcResultado = new GregorianCalendar();
        gcResultado.set(Calendar.DAY_OF_MONTH, 1);
        gcResultado.set(Calendar.YEAR, gcParam.get(Calendar.YEAR));
        gcResultado.set(Calendar.MONTH, gcParam.get(Calendar.MONTH) + 1);

        gcResultado.add(Calendar.DAY_OF_MONTH, -1);

        return gcResultado.getTime();
    }

    public static java.util.Date unMesAtras(java.util.Date date) {
        GregorianCalendar gcParam = new GregorianCalendar();
        gcParam.setTime(date);

        GregorianCalendar gcResultado = new GregorianCalendar();
        gcResultado.set(Calendar.DAY_OF_MONTH, gcParam.get(Calendar.DAY_OF_MONTH));
        gcResultado.set(Calendar.YEAR, gcParam.get(Calendar.YEAR));
        gcResultado.set(Calendar.MONTH, gcParam.get(Calendar.MONTH) -1);

        //gcResultado.add(Calendar.DAY_OF_MONTH, -1);

        return gcResultado.getTime();
    }

    public static java.util.Date unMesAdelante(java.util.Date date) {
        GregorianCalendar gcParam = new GregorianCalendar();
        gcParam.setTime(date);

        GregorianCalendar gcResultado = new GregorianCalendar();
        gcResultado.set(Calendar.DAY_OF_MONTH, gcParam.get(Calendar.DAY_OF_MONTH));
        gcResultado.set(Calendar.YEAR, gcParam.get(Calendar.YEAR));
        gcResultado.set(Calendar.MONTH, gcParam.get(Calendar.MONTH) +1);

        //gcResultado.add(Calendar.DAY_OF_MONTH, -1);

        return gcResultado.getTime();
    }

    public static java.util.Date finalDelDia(java.util.Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }

    /**
     *
     * @param fechaInicial
     * @param diasPedidos
     * @param
     * @param listaFechasNoLaborables
     * @return devuelve la cantidad de dias habiles
     */
    public static java.util.Date diasHabiles(Calendar fechaInicial, int diasPedidos, List<java.util.Date> listaFechasNoLaborables) {
        int totalDiasFeriados = 0;
        boolean feriado = false;

        while (diasPedidos > 0) {

            if (!listaFechasNoLaborables.isEmpty()) {
                for (int i = 0; i < listaFechasNoLaborables.size(); i++) {
                    if (listaFechasNoLaborables.get(i).compareTo(fechaInicial.getTime()) == 0) {
                        fechaInicial.add(Calendar.DATE, 1);
                        feriado = true;
                        break;
                    } else {
                        feriado = false;
                    }

                }

            }
            System.out.println("fecha inicial " + fechaInicial.getTime());
            if (!feriado) {
                if (fechaInicial.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && fechaInicial.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                    diasPedidos--;
                    if (diasPedidos == 0) {
                        break;
                    }
                    fechaInicial.add(Calendar.DATE, 1);

                } else {
                    fechaInicial.add(Calendar.DATE, 1);
                }
            }

        }

        return fechaInicial.getTime();
    }

    /**
     * Devuelve la fecha sola en un objeto Date
     *
     * @return valor de fecha con hora 00:00:00
     */
    public java.util.Date getFechaSinHora() {

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        java.util.Date hoy = this.getTime();
        try {
            hoy = sdf.parse(sdf.format(hoy));
        } catch (ParseException ex) {
        }
        return hoy;
    }

    public String getNombreDia() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEEEEE");
        String resultado = sdf.format(this.getTime());
        resultado = resultado.substring(0, 1).toUpperCase() + resultado.substring(1);
        return resultado;
    }

    public String getNombreMes() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMMMM");
        String resultado = sdf.format(this.getTime());
        resultado = resultado.substring(0, 1).toUpperCase() + resultado.substring(1);
        return resultado;
    }

    public String toString(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(this.getTime());
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat(DEFAULT_PATTERN);
        return sdf.format(this.getTime());
    }

    public XMLGregorianCalendar toXMLGregorianCalendar() {
        XMLGregorianCalendar resultado = null;
        try {
            DatatypeFactory df = DatatypeFactory.newInstance();
            resultado = df.newXMLGregorianCalendar(this);

        } catch (Exception exc) {
            resultado = null;
        }

        return resultado;
    }
}
