package com.yourcompany.sbif.datalake
        
import com.huemulsolutions.bigdata.common._
import com.huemulsolutions.bigdata.control._
import com.huemulsolutions.bigdata.datalake._
import org.apache.spark.sql.types._
import com.yourcompany.settings.globalSettings._

//ESTE CODIGO FUE GENERADO A PARTIR DEL TEMPLATE DEL SITIO WEB

/**
 * Clase que permite abrir un archivo de texto, devuelve un objeto huemul_dataLake con un DataFrame de los datos
 */
class raw_B1_mes(huemulBigDataGov: huemul_BigDataGovernance, Control: huemul_Control) extends huemul_DataLake(huemulBigDataGov, Control) with Serializable  {
   this.Description = "Decripción de la interfaz"
   this.GroupName = "sbif"
   this.setFrequency(huemulType_Frequency.MONTHLY)
   
   //Crea variable para configuración de lectura del archivo
   val CurrentSetting: huemul_DataLakeSetting = new huemul_DataLakeSetting(huemulBigDataGov)
   //setea la fecha de vigencia de esta configuración
     .setStartDate(2010,1,1,0,0,0)
     .setEndDate(2050,12,12,0,0,0)

   //Configuración de rutas globales
     .setGlobalPath(huemulBigDataGov.GlobalSettings.RAW_SmallFiles_Path)
   //Configura ruta local, se pueden usar comodines
     .setLocalPath("sbif/{{YYYY}}{{MM}}/")
   //configura el nombre del archivo (se pueden usar comodines)
     .setFileName("b1{{YYYY}}{{MM}}{{Cod_Banco}}.txt")
   //especifica el tipo de archivo a leer
     .setFileType(huemulType_FileType.TEXT_FILE)
   //expecifica el nombre del contacto del archivo en TI
     .setContactName("SBIF")

   //Indica como se lee el archivo
     .setColumnDelimiterType(huemulType_Separator.CHARACTER)  //POSITION;CHARACTER
   //separador de columnas
     .setColumnDelimiter("\t")    //SET FOR CARACTER
   //forma rápida de configuración de columnas del archivo
   //CurrentSetting.DataSchemaConf.setHeaderColumnsString("institucion_id;institucion_nombre")
   //Forma detallada
     .addColumn("planCuenta_id", "planCuenta_id", StringType, "Código contable. Es un campo de 7 digitos que identifica el concepto contable que se describe en el archivo Modelo-MB1.txt.")
     .addColumn("b1_Monto_clp", "monto_clp", DecimalType(20,2), "Monto Moneda Chilena No Reajustable (Valor en millones de pesos chilenos)")
     .addColumn("b1_Monto_ipc", "monto_ipc", DecimalType(20,2), "Monto Moneda reajustable por factores de IPC (Valor en millones de pesos chilenos)")
     .addColumn("b1_Monto_tdc", "monto_tdc", DecimalType(20,2), "Monto Moneda reajustable por Tipo de Cambio (Valor en millones de pesos chilenos)")
     .addColumn("b1_Monto_tdcb", "monto_tdcb", DecimalType(20,2), "Monto en Moneda Extranjera de acuerdo al tipo de cambio de representación contable usado por el banco (Valor en millones de pesos chilenos)")
    
   //Seteo de lectura de información de Log (en caso de tener)
     .setHeaderColumnDelimiterType(huemulType_Separator.CHARACTER)  //POSITION;CHARACTER;NONE
     .setLogNumRowsColumnName(null)
     .setHeaderColumnDelimiter("\t")   //SET FOR CARACTER
     .setHeaderColumnsString("codigo;banco")

   this.SettingByDate.append(CurrentSetting)
  
    /***
   * open(ano: Int, mes: Int) <br>
   * método que retorna una estructura con un DF de detalle, y registros de control <br>
   * ano: año de los archivos recibidos <br>
   * mes: mes de los archivos recibidos <br>
   * dia: dia de los archivos recibidos <br>
   * Retorna: true si está OK, false si tuvo algún problema <br>
  */
  def open(Alias: String, ControlParent: huemul_Control, ano: Integer, mes: Integer, dia: Integer, hora: Integer, min: Integer, seg: Integer, Cod_Banco: String): Boolean = {
    //Crea registro de control de procesos
     val control = new huemul_Control(huemulBigDataGov, ControlParent, huemulType_Frequency.MONTHLY)
    //Guarda los parámetros importantes en el control de procesos
    control.AddParamYear("Ano", ano)
    control.AddParamMonth("Mes", mes)
    control.AddParamInformation("Cod_Banco", Cod_Banco)
       
    try { 
      //NewStep va registrando los pasos de este proceso, también sirve como documentación del mismo.
      control.NewStep("Abre archivo RDD y devuelve esquemas para transformar a DF")
      if (!this.OpenFile(ano, mes, dia, hora, min, seg, s"{{Cod_Banco}}=$Cod_Banco")){
        //Control también entrega mecanismos de envío de excepciones
        control.RaiseError(s"Error al abrir archivo: ${this.Error.ControlError_Message}")
      }
   
      control.NewStep("Aplicando Filtro y leyendo formato")
      //Si el archivo no tiene cabecera, comentar la línea de .filter
      val rowRDD = this.DataRDD
            .filter { x => x != this.Log.DataFirstRow  }
            .map { x => this.ConvertSchema(x) }
            
      control.NewStep("Transformando datos a dataframe")      
      //Crea DataFrame en Data.DataDF
      this.DF_from_RAW(rowRDD, Alias)
        
      //****VALIDACION DQ*****
      //**********************
      control.NewStep("Valida que cantidad de registros esté entre 10 y 1000")    
      //validacion cantidad de filas
      val validanumfilas = this.DataFramehuemul.DQ_NumRowsInterval(this, 10, 1000)      
      if (validanumfilas.isError) control.RaiseError(s"user: Numero de Filas fuera del rango. ${validanumfilas.Description}")
      
      control.NewStep("Valida que codigo dentro del archivo sea igual al parámetro recibido")    
      //validacion cantidad de filas
      val CodBancoLog = this.Log.LogDF.first().getAs[String]("codigo")
      if (CodBancoLog != Cod_Banco)
        control.RaiseError(s"user: Código de institucion del archivo $CodBancoLog es distinto al código de institución del parámetro $Cod_Banco")
      
      
      control.FinishProcessOK                      
    } catch {
      case e: Exception =>
        control.Control_Error.GetError(e, this.getClass.getName, null)
        control.FinishProcessError()   

    }         
    control.Control_Error.IsOK()
  }
}


/**
 * Este objeto se utiliza solo para probar la lectura del archivo RAW
 * La clase que está definida más abajo se utiliza para la lectura.
 */
object raw_B1_mes_test {
   /**
   * El proceso main es invocado cuando se ejecuta este código
   * Permite probar la configuración del archivo RAW
   */
  
  def main(args : Array[String]) {
    
    //Creación API
    val huemulBigDataGov  = new huemul_BigDataGovernance(s"Testing DataLake - ${this.getClass.getSimpleName}", args, Global)
    //Creación del objeto control, por default no permite ejecuciones en paralelo del mismo objeto (corre en modo SINGLETON)
    val Control = new huemul_Control(huemulBigDataGov, null, huemulType_Frequency.ANY_MOMENT)

    /** ************* PARAMETROS **********************/
    val param_ano = huemulBigDataGov.arguments.GetValue("ano", null, "Debe especificar el parámetro año, ej: ano=2017").toInt
    val param_mes = huemulBigDataGov.arguments.GetValue("mes", null, "Debe especificar el parámetro mes, ej: mes=12").toInt
    
    //Inicializa clase RAW  
    val DF_RAW =  new raw_B1_mes(huemulBigDataGov, Control)
    if (!DF_RAW.open("DF_RAW", null, param_ano, param_mes, 0, 0, 0, 0, "001")) {
      println("************************************************************")
      println("**********  E  R R O R   E N   P R O C E S O   *************")
      println("************************************************************")
    } else
      DF_RAW.DataFramehuemul.DataFrame.show()
      
    Control.FinishProcessOK
   
  }  
}
