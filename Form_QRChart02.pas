unit Form_QRChart02;

interface

uses
  Windows, Messages, SysUtils, Variants, Classes, Graphics, Controls, Forms,
  Dialogs, QRCtrls, QuickRpt, ExtCtrls, DB;

type
  TFormQRChart02 = class(TForm)
    QuickRep1: TQuickRep;
    PageFooterBand1: TQRBand;
    DetailBand1: TQRBand;
    QRShape55: TQRShape;
    QRShape54: TQRShape;
    QRShape15: TQRShape;
    QRShape12: TQRShape;
    QRShape122: TQRShape;
    QRShape58: TQRShape;
    QRShape57: TQRShape;
    QRShape56: TQRShape;
    QRShape25: TQRShape;
    QRShape21: TQRShape;
    QRLabel11: TQRLabel;
    QRShape16: TQRShape;
    QRShape17: TQRShape;
    QRShape18: TQRShape;
    QRShape19: TQRShape;
    QRShape20: TQRShape;
    QRLabel13: TQRLabel;
    QRShape22: TQRShape;
    QRShape23: TQRShape;
    QRShape24: TQRShape;
    QRLabel14: TQRLabel;
    QRLabel15: TQRLabel;
    QRLabel16: TQRLabel;
    Qlb3_FDATE: TQRLabel;
    QRLabel18: TQRLabel;
    QRLabel17: TQRLabel;
    QRLabel19: TQRLabel;
    Qsp3_SP001: TQRShape;
    QRLabel20: TQRLabel;
    Qsp3_SP002: TQRShape;
    QRLabel21: TQRLabel;
    Qsp3_SP003: TQRShape;
    QRLabel22: TQRLabel;
    Qsp3_SP004: TQRShape;
    QRLabel23: TQRLabel;
    Qsp3_SP005: TQRShape;
    QRLabel24: TQRLabel;
    QRShape26: TQRShape;
    QRLabel26: TQRLabel;
    QRShape27: TQRShape;
    QRShape28: TQRShape;
    QRShape29: TQRShape;
    QRShape30: TQRShape;
    QRShape31: TQRShape;
    Qlb3_GNM01: TQRLabel;
    QRShape32: TQRShape;
    QRShape33: TQRShape;
    QRShape34: TQRShape;
    QRLabel28: TQRLabel;
    Qlb3_GNM02: TQRLabel;
    QRLabel30: TQRLabel;
    QRShape35: TQRShape;
    QRLabel31: TQRLabel;
    QRLabel32: TQRLabel;
    QRLabel33: TQRLabel;
    QRLabel34: TQRLabel;
    QRShape36: TQRShape;
    QRShape37: TQRShape;
    QRLabel35: TQRLabel;
    QRShape38: TQRShape;
    Qlb3_GNM03: TQRLabel;
    Qlb3_GNM04: TQRLabel;
    Qlb3_GNM05: TQRLabel;
    Qlb3_GNM06: TQRLabel;
    Qlb3_GNM07: TQRLabel;
    Qlb3_GNM08: TQRLabel;
    Qlb3_GNM09: TQRLabel;
    Qlb3_GNM10: TQRLabel;
    QRLabel44: TQRLabel;
    QRLabel45: TQRLabel;
    QRLabel46: TQRLabel;
    QRLabel47: TQRLabel;
    QRLabel48: TQRLabel;
    QRLabel49: TQRLabel;
    QRLabel50: TQRLabel;
    QRShape39: TQRShape;
    Qlb3_GNM11: TQRLabel;
    Qlb3_GNM13: TQRLabel;
    QRLabel53: TQRLabel;
    Qlb3_GNM12: TQRLabel;
    Qsp3_SP006: TQRShape;
    QRLabel55: TQRLabel;
    Qsp3_SP007: TQRShape;
    QRLabel56: TQRLabel;
    QRShape42: TQRShape;
    QRShape43: TQRShape;
    QRShape44: TQRShape;
    QRShape45: TQRShape;
    QRShape46: TQRShape;
    QRShape47: TQRShape;
    QRLabel57: TQRLabel;
    QRShape48: TQRShape;
    QRLabel58: TQRLabel;
    QRShape49: TQRShape;
    QRLabel59: TQRLabel;
    QRLabel60: TQRLabel;
    QRLabel61: TQRLabel;
    QRLabel62: TQRLabel;
    QRLabel63: TQRLabel;
    QRLabel64: TQRLabel;
    QRLabel65: TQRLabel;
    QRLabel66: TQRLabel;
    QRLabel67: TQRLabel;
    QRShape50: TQRShape;
    QRLabel68: TQRLabel;
    QRShape51: TQRShape;
    QRLabel69: TQRLabel;
    QRShape52: TQRShape;
    QRLabel70: TQRLabel;
    QRShape53: TQRShape;
    QRLabel71: TQRLabel;
    QRLabel72: TQRLabel;
    QRLabel73: TQRLabel;
    QRLabel74: TQRLabel;
    QRLabel75: TQRLabel;
    QRLabel76: TQRLabel;
    QRShape59: TQRShape;
    QRLabel77: TQRLabel;
    QRLabel78: TQRLabel;
    QRLabel79: TQRLabel;
    QRShape60: TQRShape;
    QRShape61: TQRShape;
    QRShape62: TQRShape;
    QRShape63: TQRShape;
    QRLabel80: TQRLabel;
    QRLabel81: TQRLabel;
    QRLabel82: TQRLabel;
    QRLabel83: TQRLabel;
    QRLabel84: TQRLabel;
    QRLabel85: TQRLabel;
    QRShape64: TQRShape;
    Qsp3_JD000: TQRShape;
    QRLabel86: TQRLabel;
    Qsp3_JD001: TQRShape;
    QRLabel87: TQRLabel;
    Qsp3_YC000: TQRShape;
    QRLabel88: TQRLabel;
    Qsp3_YC001: TQRShape;
    QRLabel89: TQRLabel;
    QRShape69: TQRShape;
    Qsp3_SP008: TQRShape;
    QRLabel90: TQRLabel;
    Qsp3_ECK00: TQRShape;
    QRLabel91: TQRLabel;
    Qsp3_ECK01: TQRShape;
    QRLabel92: TQRLabel;
    QRShape73: TQRShape;
    Qsp3_1NJJD: TQRShape;
    QRLabel93: TQRLabel;
    Qsp3_1SJJD: TQRShape;
    QRLabel94: TQRLabel;
    Qsp3_1GHJD: TQRShape;
    QRLabel95: TQRLabel;
    Qsp3_1DNJD: TQRShape;
    QRLabel96: TQRLabel;
    Qsp3_1JJJD: TQRShape;
    QRLabel97: TQRLabel;
    Qsp3_1PHJD: TQRShape;
    QRLabel98: TQRLabel;
    Qsp3_1ETJD: TQRShape;
    QRLabel99: TQRLabel;
    Qsp3_1NJYC: TQRShape;
    QRLabel100: TQRLabel;
    Qsp3_1SJYC: TQRShape;
    QRLabel101: TQRLabel;
    Qsp3_1GHYC: TQRShape;
    QRLabel102: TQRLabel;
    Qsp3_1DNYC: TQRShape;
    QRLabel103: TQRLabel;
    Qsp3_1JJYC: TQRShape;
    QRLabel104: TQRLabel;
    Qsp3_1PHYC: TQRShape;
    QRLabel105: TQRLabel;
    Qsp3_1ETYC: TQRShape;
    QRLabel106: TQRLabel;
    Qsp3_2NJCK: TQRShape;
    QRLabel107: TQRLabel;
    Qsp3_2SJCK: TQRShape;
    QRLabel108: TQRLabel;
    Qsp3_2GHCK: TQRShape;
    QRLabel109: TQRLabel;
    Qsp3_2DNCK: TQRShape;
    QRLabel110: TQRLabel;
    Qsp3_2ETCK: TQRShape;
    QRLabel113: TQRLabel;
    Qsp3_SP009: TQRShape;
    QRLabel111: TQRLabel;
    Qsp3_SP010: TQRShape;
    QRLabel112: TQRLabel;
    Qsp3_SP011: TQRShape;
    QRLabel114: TQRLabel;
    QRShape97: TQRShape;
    QRLabel116: TQRLabel;
    QRLabel117: TQRLabel;
    QRLabel118: TQRLabel;
    QRLabel119: TQRLabel;
    Qsp3_SP012: TQRShape;
    QRLabel120: TQRLabel;
    Qsp3_SP013: TQRShape;
    QRLabel121: TQRLabel;
    Qsp3_SP014: TQRShape;
    QRShape99: TQRShape;
    QRLabel122: TQRLabel;
    QRLabel123: TQRLabel;
    QRLabel124: TQRLabel;
    QRLabel125: TQRLabel;
    Qlb3_PTCNO: TQRLabel;
    Qlb3_PTNAM: TQRLabel;
    Qlb3_PTAGE: TQRLabel;
    Qlb3_BIRDT: TQRLabel;
    QRShape72: TQRShape;
    QRShape1: TQRShape;
    QRShape2: TQRShape;
    QRLabel1: TQRLabel;
    Qsp3_BAL01: TQRShape;
    QRLabel2: TQRLabel;
    Qsp3_BAL02: TQRShape;
    QRLabel3: TQRLabel;
    Qsp3_BAL03: TQRShape;
    QRLabel4: TQRLabel;
    Qsp3_BAL04: TQRShape;
    QRLabel5: TQRLabel;
    QMm3_JMEMO: TQRMemo;
    QRLabel6: TQRLabel;
    QRShape7: TQRShape;
    QRShape8: TQRShape;
    QRShape9: TQRShape;
    QRShape10: TQRShape;
    QRShape11: TQRShape;
    QRShape13: TQRShape;
    QRLabel7: TQRLabel;
    Qsp3_SP015: TQRShape;
    QRShape14: TQRShape;
    QRLabel8: TQRLabel;
    Qsp3_3BGEA: TQRShape;
    QRLabel9: TQRLabel;
    Qsp3_3BGEB: TQRShape;
    QRLabel10: TQRLabel;
    QRShape83: TQRShape;
    Qsp3_3BGEC: TQRShape;
    QRLabel12: TQRLabel;
    QRShape85: TQRShape;
    QRLabel25: TQRLabel;
    Qsp3_SKEVA: TQRShape;
    QRLabel115: TQRLabel;
    Qsp3_SKEVB: TQRShape;
    QRLabel157: TQRLabel;
    Qsp3_SKEVC: TQRShape;
    QRLabel158: TQRLabel;
    Qsp3_SKEVD: TQRShape;
    QRLabel171: TQRLabel;
    QRLabel129: TQRLabel;
    QRShape123: TQRShape;
    QRShape124: TQRShape;
    QRShape125: TQRShape;
    Qig3_MKIMG: TQRImage;
    QRShape126: TQRShape;
    QRShape127: TQRShape;
    QRShape109: TQRShape;
    QRShape110: TQRShape;
    QRShape111: TQRShape;
    QRShape65: TQRShape;
    QRShape115: TQRShape;
    QRLabel40: TQRLabel;
    QRShape112: TQRShape;
    QRShape3: TQRShape;
    QRShape4: TQRShape;
    QRShape5: TQRShape;
    QRShape6: TQRShape;
    QRShape40: TQRShape;
    QRShape41: TQRShape;
    QRShape66: TQRShape;
    QRShape67: TQRShape;
    QRShape68: TQRShape;
    QRShape71: TQRShape;
    QRShape75: TQRShape;
    QRShape74: TQRShape;
    QRShape70: TQRShape;
    QRShape76: TQRShape;
    QRShape77: TQRShape;
    QRShape78: TQRShape;
    QRShape79: TQRShape;
    QRShape80: TQRShape;
    QRShape81: TQRShape;
    QRShape82: TQRShape;
    QRShape84: TQRShape;
    QRShape86: TQRShape;
    QRShape87: TQRShape;
    QRShape88: TQRShape;
    QRShape89: TQRShape;
    QRShape90: TQRShape;
    QRShape91: TQRShape;
    Qlb3_GNM61: TQRLabel;
    Qlb3_GNM62: TQRLabel;
    QRShape92: TQRShape;
    Qlb3_GNM63: TQRLabel;
    Qlb3_GNM64: TQRLabel;
    QRShape93: TQRShape;
    QRShape94: TQRShape;
    QRLabel38: TQRLabel;
    Qlb3_GNM65: TQRLabel;
    QRLabel41: TQRLabel;
    QRLabel42: TQRLabel;
    QRLabel43: TQRLabel;
    QRShape95: TQRShape;
    QRShape96: TQRShape;
    QRShape98: TQRShape;
    QRShape100: TQRShape;
    QRShape101: TQRShape;
    QRShape102: TQRShape;
    QRShape103: TQRShape;
    QRShape104: TQRShape;
    QRShape105: TQRShape;
    QRShape107: TQRShape;
    QRShape108: TQRShape;
    QRShape113: TQRShape;
    QRShape114: TQRShape;
    QRShape116: TQRShape;
    QRShape117: TQRShape;
    QRShape106: TQRShape;
    QRShape118: TQRShape;
    QRShape119: TQRShape;
    QRShape120: TQRShape;
    QRShape121: TQRShape;
    QRShape128: TQRShape;
    Qlb3_GNM16: TQRLabel;
    Qlb3_GNM19: TQRLabel;
    Qlb3_GNM24: TQRLabel;
    Qlb3_GNM28: TQRLabel;
    Qlb3_GNM37: TQRLabel;
    Qlb3_GNM40: TQRLabel;
    Qlb3_GNM48: TQRLabel;
    Qlb3_GNM52: TQRLabel;
    Qlb3_GNM56: TQRLabel;
    Qlb3_GNM14: TQRLabel;
    Qlb3_GNM17: TQRLabel;
    Qlb3_GNM20: TQRLabel;
    Qlb3_GNM23: TQRLabel;
    Qlb3_GNM27: TQRLabel;
    Qlb3_GNM29: TQRLabel;
    Qlb3_GNM31: TQRLabel;
    Qlb3_GNM33: TQRLabel;
    Qlb3_GNM36: TQRLabel;
    Qlb3_GNM41: TQRLabel;
    Qlb3_GNM43: TQRLabel;
    Qlb3_GNM45: TQRLabel;
    Qlb3_GNM49: TQRLabel;
    Qlb3_GNM54: TQRLabel;
    Qlb3_GNM57: TQRLabel;
    Qlb3_GNM60: TQRLabel;
    QRShape129: TQRShape;
    Qlb3_GNM35: TQRLabel;
    Qlb3_GNM39: TQRLabel;
    QRShape130: TQRShape;
    Qlb3_GNM15: TQRLabel;
    Qlb3_GNM18: TQRLabel;
    Qlb3_GNM21: TQRLabel;
    Qlb3_GNM22: TQRLabel;
    Qlb3_GNM26: TQRLabel;
    Qlb3_GNM30: TQRLabel;
    Qlb3_GNM32: TQRLabel;
    Qlb3_GNM34: TQRLabel;
    Qlb3_GNM38: TQRLabel;
    Qlb3_GNM42: TQRLabel;
    Qlb3_GNM44: TQRLabel;
    Qlb3_GNM46: TQRLabel;
    Qlb3_GNM50: TQRLabel;
    Qlb3_GNM51: TQRLabel;
    Qlb3_GNM53: TQRLabel;
    Qlb3_GNM55: TQRLabel;
    Qlb3_GNM59: TQRLabel;
    Qlb3_GNM25: TQRLabel;
    Qlb3_GNM47: TQRLabel;
    Qlb3_GNM58: TQRLabel;
    QRShape131: TQRShape;
    QRShape132: TQRShape;
    QRShape133: TQRShape;
    QRShape134: TQRShape;
    QRShape135: TQRShape;
    QRShape136: TQRShape;
    QRShape137: TQRShape;
    procedure FormClose(Sender: TObject; var Action: TCloseAction);
    procedure FormDestroy(Sender: TObject);
    procedure DetailBand1BeforePrint(Sender: TQRCustomBand;
      var PrintBand: Boolean);
  private
    procedure Pcv_ScreenClar;
    procedure Pcv_GumSelRptcd(Is_RpSub : AnsiString; Io_CpNm1 : TQRPrintable);
    { Private declarations }
  public
    Us_Jubno : AnsiString;
    Us_RptNm : AnsiString;
    Ub_Color : Boolean;
    Ub_Jumin : Boolean;
    Ui_JUBGN : Integer;
    { Public declarations }
  end;

var
  FormQRChart02: TFormQRChart02;

implementation

uses Modu_DbsEngine,Unit_DbUsesGrp, Unit_FctionGrp, Unit_EncDecUse;

{$R *.dfm}

procedure TFormQRChart02.FormClose(Sender: TObject;
  var Action: TCloseAction);
begin
  ModuDbsEngine.Pcb_AConnClose('2');
  Action := Cafree;
end;

procedure TFormQRChart02.FormDestroy(Sender: TObject);
begin
  FormQRChart02 := Nil;
end;

Procedure TFormQRChart02.Pcv_ScreenClar;
Var
  Mi_CpCnt : Integer;
Begin
  For Mi_CpCnt := 0 To Self.ComponentCount - 1 Do
  Begin
    If Self.Components[Mi_CpCnt] Is TQRLabel Then
    Begin
      If (UpperCase(Copy(TQRLabel(Self.Components[Mi_CpCnt]).Name, 1, 5)) = 'QLB3_' ) And
         (UpperCase(Copy(TQRLabel(Self.Components[Mi_CpCnt]).Name, 1, 7)) <> 'QLB3_GN') Then
      Begin
        TQRLabel(Self.Components[Mi_CpCnt]).Caption := '';
      End;
    End Else
    If Self.Components[Mi_CpCnt] Is TQRShape Then
    Begin
      If UpperCase(Copy(TQRLabel(Self.Components[Mi_CpCnt]).Name, 1, 5)) = 'QSP3_' Then
      Begin
        TQRShape(Self.Components[Mi_CpCnt]).Brush.Color := clWhite;
      End;
    End Else
    If Self.Components[Mi_CpCnt] Is TQRMemo Then
    Begin
      If UpperCase(Copy(TQRMemo(Self.Components[Mi_CpCnt]).Name, 1, 5)) = 'QMM3_' Then
      Begin
        TQRMemo(Self.Components[Mi_CpCnt]).Lines.Clear;
      End;
    End;
  End;
End;



procedure TFormQRChart02.DetailBand1BeforePrint(Sender: TQRCustomBand;
  var PrintBand: Boolean);
var
  Ms_ExRlt : AnsiString;
  Ms_41SEM, Ms_41SMK, Ms_51ECG, Ms_51SMK : AnsiString;
  Ms_PAgeY, Ms_JuSex : AnsiString;

  Ms_TblN1, Ms_FldN1 : AnsiString;
  Ms_TblN2, Ms_FldN2 : AnsiString;
  Ms_TblN3, Ms_FldN3 : AnsiString;
begin
Pcv_ScreenClar;

  If Ui_JUBGN = 0 Then  //접수
  Begin
    Ms_TblN1 := '' + As_Dmain + 'JUBSUTABLE';
    Ms_FldN1 := 'JUBS';
    Ms_TblN2 := '' + As_Dmain + 'JUBGKTABLE';
    Ms_FldN2 := 'JUBG';
    Ms_TblN3 := '' + As_Dmain + 'JUBEMTABLE';
    Ms_FldN3 := 'JUBE';
    //Qlb3_JUBGN.Caption := '(접수)';
  End Else
  If Ui_JUBGN = 1 Then  //예약
  Begin
    Ms_TblN1 := '' + As_Dmain + 'PMSSUTABLE';
    Ms_FldN1 := 'PMSS';
    Ms_TblN2 := '' + As_Dmain + 'PMSGKTABLE';
    Ms_FldN2 := 'PMSG';
    Ms_TblN3 := '' + As_Dmain + 'PMSEMTABLE';
    Ms_FldN3 := 'PMSE';
    //Qlb3_JUBGN.Caption := '(예약)';
  End;
  
  //인적
  With ModuDbsEngine.Cds2_Qury1 Do
  Begin
    Am_11SQL.Clear; Am_11PNM.Clear; Am_11PVL.Clear;
    Am_11SQL.Text := 'Select ' + #13#10 +
                     '  Juutle.'+Ms_FldN1+'JUBNO, Juutle.'+Ms_FldN1+'FDATE, Juutle.'+Ms_FldN1+'FJBNO, Juutle.'+Ms_FldN1+'PTNAM, Juutle.'+Ms_FldN1+'PTCNO, Juutle.'+Ms_FldN1+'BIRDT ' + #13#10 +
                     ', Juutle.'+Ms_FldN1+'PHON1, Juutle.'+Ms_FldN1+'HANDP, Juutle.'+Ms_FldN1+'JIKYK, Juutle.'+Ms_FldN1+'EMAIL, Juutle.'+Ms_FldN1+'ZIPN1, Juutle.'+Ms_FldN1+'ZIPN2 ' + #13#10 +
                     ', Juutle.'+Ms_FldN1+'JUSO1, Juutle.'+Ms_FldN1+'JUSO2, Juutle.'+Ms_FldN1+'JUSO3, Juutle.'+Ms_FldN1+'NCITY, Juutle.'+Ms_FldN1+'BLDNO, Juutle.'+Ms_FldN1+'ZIPN3 ' + #13#10 +
                     ', Juutle.'+Ms_FldN1+'COMNO, Juutle.'+Ms_FldN1+'BRNNO, Juutle.'+Ms_FldN1+'LFCOD, Juutle.'+Ms_FldN1+'MDRNO, Juutle.'+Ms_FldN1+'BUSEO ' + #13#10 +
                     ', Juutle.'+Ms_FldN1+'GOWOO, Juutle.'+Ms_FldN1+'MOR40, Juutle.'+Ms_FldN1+'GOSAE ' + #13#10 + //정신건강, 인지기능, 생활습관
                     ', Pattle.PATIJUMN1, Pattle.PATIJUMN2, Pattle.PATILFCOD, Pattle.PATIBOJU1, Pattle.PATIBOJU2, Pattle.PATIBIRDT ' + #13#10 +
                     ', Pattle.PATIJUSEX, Pattle.PATIJUMNE ' + #13#10 +
                     ', Csrtle.CSTMCOMNM, Juutle.'+Ms_FldN1+'JMEMO, Juutle.'+Ms_FldN1+'SNDGN, Juutle.'+Ms_FldN1+'GOSIN ' + #13#10 +
                     ', Brctle.BANHBRNNM ' + #13#10 +
                     ', Amwjtb.AMWJCHGBN, Amdjtb.AMDJDHGBN, Amgjtb.AMGJGHGBN, Amubtb.AMUBUHGBN, Amjgtb.AMJGAHGBN ' + #13#10 +  //위,대장,간,유방,자궁
                     'From ' + Ms_TblN1+' Juutle ' + #13#10 +
                     '  Inner Join ' + As_Dbase + 'PATNTTABLE Pattle On Juutle.'+Ms_FldN1+'LFCOD = Pattle.PATILFCOD ' + #13#10 +
                     '  Left  Join ' + As_Dbase + 'CSTMRTABLE Csrtle On Juutle.'+Ms_FldN1+'COMNO = Csrtle.CSTMCOMNO ' + #13#10 +
                     '  Left  Join ' + As_Dbase + 'BRANCTABLE Brctle On Juutle.'+Ms_FldN1+'COMNO = Brctle.BANHCOMNO And Juutle.'+Ms_FldN1+'BRNNO = Brctle.BANHBRNNO ' + #13#10 +
                     '  Left  Join ' + As_Dmain + 'AMWJGRESTB Amwjtb On Juutle.'+Ms_FldN1+'JUBNO = Amwjtb.AMWJJUBNO ' + #13#10 + // =---위암
                     '  Left  Join ' + As_Dmain + 'AMDJGRESTB Amdjtb On Juutle.'+Ms_FldN1+'JUBNO = Amdjtb.AMDJJUBNO ' + #13#10 + // =---대장
                     '  Left  Join ' + As_Dmain + 'AMGJGRESTB Amgjtb On Juutle.'+Ms_FldN1+'JUBNO = Amgjtb.AMGJJUBNO ' + #13#10 + // =---간
                     '  Left  Join ' + As_Dmain + 'AMUBGRESTB Amubtb On Juutle.'+Ms_FldN1+'JUBNO = Amubtb.AMUBJUBNO ' + #13#10 + // =---유방
                     '  Left  Join ' + As_Dmain + 'AMJGGRESTB Amjgtb On Juutle.'+Ms_FldN1+'JUBNO = Amjgtb.AMJGJUBNO ' + #13#10 + // =---자궁
                     ModuDbsEngine.Fcb_SQLParamAd('U', 'Where', 'Juutle.'+Ms_FldN1+'JUBNO', '=', Ms_FldN1+'JUBNO', Am_11PNM) + #13#10 +
                     '';
    ModuDbsEngine.Fcb_SQLParamVl(Am_11PVL, Ms_FldN1+'JUBNO', Us_Jubno);
    ModuDbsEngine.Fcb_SQLQueryEx('OP', 'Y', ModuDbsEngine.Cds2_Qury1, Am_11SQL, Am_11PNM, Am_11PVL);
  End;
  If ModuDbsEngine.Cds2_Qury1.Eof Then
    Exit;

  Ms_PAgeY := Unit_FctionGrp.Fcb_PersonPAge('1', ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'FDATE').AsString, ModuDbsEngine.Cds2_Qury1.FieldByName('PATIJUMN1').AsString, ModuDbsEngine.Cds2_Qury1.FieldByName('PATIJUMN2').AsString);
  Ms_JuSex := Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('PATIJUSEX').AsString);


  //메모
  QMm3_JMEMO.Lines.Text := Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'JMEMO').AsString);


  //병력번호
  Qlb3_PTCNO.Caption := Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'PTCNO').AsString);
  //검진일자
  Qlb3_FDATE.Caption := Unit_FctionGrp.Fcb_TypeFormat('D2', Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'FDATE').AsString));
 { If Qlb3_FDATE.Caption <> '' Then
  Begin
    QRLabel18.Transparent := False;
    QRLabel18.Color := $0080FFFF;
    Qlb3_FDATE.Transparent := False;
    Qlb3_FDATE.Color := $0080FFFF;
  End;  }

  //성함
  Qlb3_PTNAM.Caption := Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'PTNAM').AsString);
{  If Qlb3_PTNAM.Caption <> '' Then
  Begin
    QRLabel14.Transparent := False;
    Qlb3_PTNAM.Transparent := False;
    Qlb3_PTAGE.Transparent := False;
    QRLabel16.Transparent := False;

    QRLabel14.Color := $0080FFFF;
    Qlb3_PTNAM.Color := $0080FFFF;
    Qlb3_PTAGE.Color := $0080FFFF;
    QRLabel16.Color := $0080FFFF;
  End; }
  //나이
  Qlb3_PTAGE.Caption := Unit_FctionGrp.Fcb_PersonPAge('2', ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'FDATE').AsString,
                                                           ModuDbsEngine.Cds2_Qury1.FieldByName('PATIJUMN1').AsString,
                                                           ModuDbsEngine.Cds2_Qury1.FieldByName('PATIJUMN2').AsString);
  //생년월일
  Qlb3_BIRDT.Caption := Unit_FctionGrp.Fcb_TypeFormat('D2', Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'BIRDT').AsString));
{  If Qlb3_BIRDT.Caption <> '' Then
  Begin
    Qlb3_BIRDT.Transparent := False;
    QRLabel15.Transparent := False;

    Qlb3_BIRDT.Color := $0080FFFF;
    QRLabel15.Color := $0080FFFF;
  End;  }


  //발송구분
  If (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'SNDGN').AsString) = '일반우편') Then
    Qsp3_BAL01.Brush.Color := clBlack;
  If (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'SNDGN').AsString) = '이메일') Then
    Qsp3_BAL02.Brush.Color := clBlack;
  If (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'SNDGN').AsString) = 'SMS') Then
    Qsp3_BAL03.Brush.Color := clBlack;
  If (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'SNDGN').AsString) = '직접수령') Then
    Qsp3_BAL04.Brush.Color := clBlack;



 { If (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMWJCHGBN').AsString) = '1') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMDJDHGBN').AsString) = '1') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMGJGHGBN').AsString) = '1') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMUBUHGBN').AsString) = '1') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMJGAHGBN').AsString) = '1') Then
    Qsp3_SP006.Brush.Color := clBlack
  Else
  If (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMWJCHGBN').AsString) = '2') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMWJCHGBN').AsString) = '3') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMDJDHGBN').AsString) = '2') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMDJDHGBN').AsString) = '3') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMGJGHGBN').AsString) = '2') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMGJGHGBN').AsString) = '3') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMUBUHGBN').AsString) = '2') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMUBUHGBN').AsString) = '3') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMJGAHGBN').AsString) = '2') Or
     (Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMJGAHGBN').AsString) = '3') Then
    Qsp3_SP007.Brush.Color := clBlack;    }

  //본인부담금
  Pcv_GumSelRptcd('G07', Qsp3_SP007);
  If Qsp3_SP007.Brush.Color <> clBlack Then
    Qsp3_SP006.Brush.Color := clBlack;

  //암검진여부
  //위
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMWJCHGBN').AsString) <> '' Then
    Qsp3_SP001.Brush.Color := clBlack;
  //대장
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMDJDHGBN').AsString) <> '' Then
    Qsp3_SP002.Brush.Color := clBlack;
  //간
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMGJGHGBN').AsString) <> '' Then
    Qsp3_SP003.Brush.Color := clBlack;
  //유방
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMUBUHGBN').AsString) <> '' Then
    Qsp3_SP004.Brush.Color := clBlack;
  //자궁
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName('AMJGAHGBN').AsString) <> '' Then
    Qsp3_SP005.Brush.Color := clBlack;

  //정신건강
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'GOWOO').AsString) = 'Y' Then
    Qsp3_SP012.Brush.Color := clBlack;
  //인지기능
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'MOR40').AsString) = 'Y' Then
    Qsp3_SP013.Brush.Color := clBlack;
  //생활습관평가
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'GOSAE').AsString) = 'Y' Then
    Qsp3_SP014.Brush.Color := clBlack;
  //노인신체
  If Trim(ModuDbsEngine.Cds2_Qury1.FieldByName(Ms_FldN1+'GOSIN').AsString) = 'Y' Then
    Qsp3_SP015.Brush.Color := clBlack;


  //문진
  With ModuDbsEngine.Cds2_Qury2 Do
  Begin
    Am_11SQL.Clear; Am_11PNM.Clear; Am_11PVL.Clear;
    Am_11SQL.Text := 'Select ' + #13#10 +
                     '  Mnetle.MNONJUBNO, Mnetle.MNONSUSIN ' + #13#10 +
                     ', Mnetle.MNON1NJJD, Mnetle.MNON1SJJD, Mnetle.MNON1GHJD, Mnetle.MNON1DNJD, Mnetle.MNON1JJJD, Mnetle.MNON1PHJD, Mnetle.MNON1ETJD ' + #13#10 +
                     ', Mnetle.MNON1NJYC, Mnetle.MNON1SJYC, Mnetle.MNON1GHYC, Mnetle.MNON1DNYC, Mnetle.MNON1JJYC, Mnetle.MNON1PHYC, Mnetle.MNON1ETYC ' + #13#10 +
                     ', Mnetle.MNON2NJCK, Mnetle.MNON2SJCK, Mnetle.MNON2GHCK, Mnetle.MNON2DNCK, Mnetle.MNON2ETCK ' + #13#10 +
                     ', Mnetle.MNON41SMK, Mnetle.MNON41SEM ' + #13#10 +
                     ', Mnetle.MNON51SMK, Mnetle.MNON51ECG ' + #13#10 +
                     ', Mnetle.MNON71DAY ' + #13#10 +
                     ', Mnetle.MNON3BGEM, Mnobbl.MNOBSKEVA ' + #13#10 +
                     ' From ' +Ms_TblN1+ ' Juutle ' + #13#10 +
                     '  Inner Join ' + As_Dbase + 'PATNTTABLE Pattle On Pattle.PATILFCOD = Juutle.'+Ms_FldN1+'LFCOD ' + #13#10 +
                     '  Left  Join ' + As_Dmain + 'MNONETABLE Mnetle On Juutle.'+Ms_FldN1+'JUBNO = Mnetle.MNONJUBNO ' + #13#10 +
                     '  Left  Join ' + As_Dmain + 'MNOBESYTBL Mnobbl On Juutle.'+Ms_FldN1+'JUBNO = Mnobbl.MNOBJUBNO ' + #13#10 +  //비만
                     ModuDbsEngine.Fcb_SQLParamAd('U', 'Where', 'Juutle.'+Ms_FldN1+'JUBNO', '=',  Ms_FldN1+'JUBNO', Am_11PNM) + #13#10 +
                     '';
    ModuDbsEngine.Fcb_SQLParamVl(Am_11PVL, Ms_FldN1+'JUBNO', Us_Jubno);
    ModuDbsEngine.Fcb_SQLQueryEx('OP', 'Y', ModuDbsEngine.Cds2_Qury2, Am_11SQL, Am_11PNM, Am_11PVL);
  End;


  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1NJJD').Asstring) = '1' Then
    Qsp3_1NJJD.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1NJYC').Asstring) = '1' Then
    Qsp3_1NJYC.Brush.Color := clBlack;

  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1SJJD').Asstring) = '1' Then
    Qsp3_1SJJD.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1SJYC').Asstring) = '1' Then
    Qsp3_1SJYC.Brush.Color := clBlack;

  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1GHJD').Asstring) = '1' Then
    Qsp3_1GHJD.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1GHYC').Asstring) = '1' Then
    Qsp3_1GHYC.Brush.Color := clBlack;

  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1DNJD').Asstring) = '1' Then
    Qsp3_1DNJD.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1DNYC').Asstring) = '1' Then
    Qsp3_1DNYC.Brush.Color := clBlack;

  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1JJJD').Asstring) = '1' Then
    Qsp3_1JJJD.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1JJYC').Asstring) = '1' Then
    Qsp3_1JJYC.Brush.Color := clBlack;

  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1PHJD').Asstring) = '1' Then
    Qsp3_1PHJD.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1PHYC').Asstring) = '1' Then
    Qsp3_1PHYC.Brush.Color := clBlack;

  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1ETJD').Asstring) = '1' Then
    Qsp3_1ETJD.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1ETYC').Asstring) = '1' Then
    Qsp3_1ETYC.Brush.Color := clBlack;

  If (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1NJJD').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1SJJD').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1GHJD').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1DNJD').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1JJJD').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1PHJD').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1ETJD').Asstring) = '0' ) Then
    Qsp3_JD000.Brush.Color := clBlack;
  If (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1NJJD').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1SJJD').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1GHJD').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1DNJD').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1JJJD').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1PHJD').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1ETJD').Asstring) = '1' ) Then
    Qsp3_JD001.Brush.Color := clBlack;

  If (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1NJYC').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1SJYC').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1GHYC').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1DNYC').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1JJYC').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1PHYC').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1ETYC').Asstring) = '0' ) Then
    Qsp3_YC000.Brush.Color := clBlack;
  If (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1NJYC').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1SJYC').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1GHYC').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1DNYC').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1JJYC').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1PHYC').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON1ETYC').Asstring) = '1' ) Then
    Qsp3_YC001.Brush.Color := clBlack;

  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2NJCK').Asstring) = '1' Then
    Qsp3_2NJCK.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2SJCK').Asstring) = '1' Then
    Qsp3_2SJCK.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2GHCK').Asstring) = '1' Then
    Qsp3_2GHCK.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2DNCK').Asstring) = '1' Then
    Qsp3_2DNCK.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2ETCK').Asstring) = '1' Then
    Qsp3_2ETCK.Brush.Color := clBlack;

  If (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2NJCK').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2SJCK').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2GHCK').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2DNCK').Asstring) = '0' ) And
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2ETCK').Asstring) = '0' ) Then
    Qsp3_ECK00.Brush.Color := clBlack;
  If (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2NJCK').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2SJCK').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2GHCK').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2DNCK').Asstring) = '1' ) Or
     (Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON2ETCK').Asstring) = '1' ) Then
    Qsp3_ECK01.Brush.Color := clBlack;



  //B형간염
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON3BGEM').Asstring) = '1' Then
    Qsp3_3BGEA.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON3BGEM').Asstring) = '2' Then
    Qsp3_3BGEB.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON3BGEM').Asstring) = '3' Then
    Qsp3_3BGEC.Brush.Color := clBlack;
  //체중
 {{ If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNOBSKEVA').Asstring) = '1' Then
    Qsp3_SKEVA.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNOBSKEVA').Asstring) = '2' Then
    Qsp3_SKEVC.Brush.Color := clBlack;
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNOBSKEVA').Asstring) = '3' Then
    Qsp3_3BGED.Brush.Color := clBlack;  }
    

  Ms_41SEM := Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON41SEM').AsString); //4.평생 5갑(100개비)이상의 담배를 피운적? 1.아니오 2.예
  Ms_41SMK := Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON41SMK').AsString); //4-1. 현재 일반담배(궐련)을 피우십니까? 1.피움 2.끊음
  Ms_51ECG := Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON51ECG').AsString); //5. 궐련형 전자담배를 피운적 있습니까? 1.아니오 2.예
  Ms_51SMK := Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON51SMK').AsString); //5-1. 현재 궐련형 전자담배 피우십니까? 1.피움 2.끊음



  //금연필요
  If (Ms_41SEM = '2') And
     (Ms_41SMK = '1') Then
    Qsp3_SP008.Brush.Color := clBlack;
  //절주필요
  Ms_ExRlt := Unit_DbUsesGrp.Fcb_CalHabitA1('1',
                                            Us_Jubno,
                                            Ms_JuSex,
                                            Ms_PAgeY,
                                            ModuDbsEngine.Fcb_ServerTime);  //. 생활습관 음주 절주 계산
  If Pos('위험', Ms_ExRlt) > 0 Then
    Qsp3_SP009.Brush.Color := clBlack;

  //. 신체활동필요, 근력운동필요
  If Trim(ModuDbsEngine.Cds2_Qury2.FieldByName('MNON71DAY').AsString) <> '' Then
  Begin
    Ms_ExRlt := Unit_DbUsesGrp.Fcb_CalHabitA1('3', Us_Jubno, Ms_JuSex, Ms_PAgeY, ModuDbsEngine.Fcb_ServerTime);  //. 생활습관 음주 절주 계산 신체활동 근력운동
    //신체활동필요
    If Pos('신체활동 부족', Ms_ExRlt) > 0 Then
      Qsp3_SP010.Brush.Color := clBlack;
    //근력운동필요
    If Pos('근력운동부족', Ms_ExRlt) > 0 Then
      Qsp3_SP011.Brush.Color := clBlack;
  End;


  //병원명칭
  With ModuDbsEngine.Cds2_Qury3 Do
  Begin
    Unit_DbUsesGrp.Fcb_ProvChange('S2');  //오라클공급자 MSDAORA OraOLEDB.Oracle 잠시변경후 원복
    Am_11SQL.Clear; Am_11PNM.Clear; Am_11PVL.Clear;
    Am_11SQL.Text := 'Select BCHSRPTNM, BCHSDEPNM, BCHSJIIMG, BCHSMKIMG, BCHSPHON1, BCHSFAXN1 ' + #13#10 +
                     '     , BCHSJUSO1, BCHSJUSO2, BCHSJUSO3 ' + #13#10 +
                     'From ' + As_Dbase + 'BCHSPINFTB ' + #13#10 +
                     ModuDbsEngine.Fcb_SQLParamAd('U', 'Where', '', '=', 'BCHSJBGBN', Am_11PNM) + #13#10 +
                     '';
    ModuDbsEngine.Fcb_SQLParamVl(Am_11PVL, 'BCHSJBGBN', 'Y');
    ModuDbsEngine.Fcb_SQLQueryEx('OP', 'Y', ModuDbsEngine.Cds2_Qury3, Am_11SQL, Am_11PNM, Am_11PVL);


    //병원로고
    Unit_FctionGrp.Pcb_JpgLoadQIm(TBLOBField(ModuDbsEngine.Cds2_Qury3.FieldByName('BCHSMKIMG')), Qig3_MKIMG);
    Unit_DbUsesGrp.Fcb_ProvChange('E2');  //오라클공급자 MSDAORA OraOLEDB.Oracle 잠시변경후 원복
  End;

   //신장
  Pcv_GumSelRptcd('A00', Qlb3_GNM01);
  //체중
  Pcv_GumSelRptcd('A01', Qlb3_GNM02);
  //체질량지수
  Pcv_GumSelRptcd('A02', Qlb3_GNM03);
  //허리둘레
  Pcv_GumSelRptcd('A03', Qlb3_GNM04);
  //혈압
  Pcv_GumSelRptcd('A04', Qlb3_GNM11);
  //시력
  Pcv_GumSelRptcd('A05', Qlb3_GNM12);
  //청력
  Pcv_GumSelRptcd('A06', Qlb3_GNM13);

  If (Qlb3_GNM01.Color = $00FF80FF) or
     (Qlb3_GNM02.Color = $00FF80FF) or
     (Qlb3_GNM03.Color = $00FF80FF) or
     (Qlb3_GNM04.Color = $00FF80FF) or
     (Qlb3_GNM11.Color = $00FF80FF) or
     (Qlb3_GNM12.Color = $00FF80FF) or
     (Qlb3_GNM13.Color = $00FF80FF) Then
  Begin
    QRLabel26.Transparent := False;
    QRLabel28.Transparent := False;
    QRLabel26.Color := $00FF80FF;
    QRLabel28.Color := $00FF80FF;
  End;


  //하지기능
  Pcv_GumSelRptcd('A07', Qlb3_GNM08);
  //평형성
  Pcv_GumSelRptcd('A08', Qlb3_GNM09);
  //보행장애
  Pcv_GumSelRptcd('A09', Qlb3_GNM10);
  If (Qlb3_GNM08.Color = $00FF80FF) or
     (Qlb3_GNM09.Color = $00FF80FF) or
     (Qlb3_GNM10.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM05.Transparent := False;
    Qlb3_GNM06.Transparent := False;
    Qlb3_GNM07.Transparent := False;
    Qlb3_GNM05.Color := $00FF80FF;
    Qlb3_GNM06.Color := $00FF80FF;
    Qlb3_GNM07.Color := $00FF80FF;
  End;





  //체지방(인바디)
  Pcv_GumSelRptcd('B00', Qlb3_GNM14);
  //안저검사
  Pcv_GumSelRptcd('B01', Qlb3_GNM17);
  //HRVII
  Pcv_GumSelRptcd('B02', Qlb3_GNM20);
  //기초검사실
  If (Qlb3_GNM14.Color = $00FF80FF) or
     (Qlb3_GNM17.Color = $00FF80FF) or
     (Qlb3_GNM20.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM16.Transparent := False;
    Qlb3_GNM16.Color := $00FF80FF;
    Qlb3_GNM19.Transparent := False;
    Qlb3_GNM19.Color := $00FF80FF;
  End;

  //부인과검사실
  Pcv_GumSelRptcd('B03', Qlb3_GNM23);
  //자궁경부암검사
  If (Qlb3_GNM23.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM24.Transparent := False;
    Qlb3_GNM24.Color := $00FF80FF;
    Qlb3_GNM28.Transparent := False;
    Qlb3_GNM28.Color := $00FF80FF;
  End;

  //혈액검사
  Pcv_GumSelRptcd('B04', Qlb3_GNM31);
  //소변검사
  Pcv_GumSelRptcd('B05', Qlb3_GNM33);
  //유전자검사
  Pcv_GumSelRptcd('B06', Qlb3_GNM36);
  //NK면역세포
  Pcv_GumSelRptcd('B07', Qlb3_GNM41);
  //지연성알레르기검사
  Pcv_GumSelRptcd('B08', Qlb3_GNM43);
  //마스토체크
  Pcv_GumSelRptcd('B09', Qlb3_GNM45);
  //임상병리센터
  If (Qlb3_GNM31.Color = $00FF80FF) or
     (Qlb3_GNM33.Color = $00FF80FF) or
     (Qlb3_GNM36.Color = $00FF80FF) or
     (Qlb3_GNM41.Color = $00FF80FF) or
     (Qlb3_GNM43.Color = $00FF80FF) or
     (Qlb3_GNM45.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM37.Transparent := False;
    Qlb3_GNM37.Color := $00FF80FF;
    Qlb3_GNM40.Transparent := False;
    Qlb3_GNM40.Color := $00FF80FF;
  End;

  //심전도
  Pcv_GumSelRptcd('C00', Qlb3_GNM49);
  //심전도실
  If (Qlb3_GNM49.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM48.Transparent := False;
    Qlb3_GNM48.Color := $00FF80FF;
  End;

  //수면
  Pcv_GumSelRptcd('C01', Qlb3_GNM61);
  //위내시경
  Pcv_GumSelRptcd('C02', Qlb3_GNM63);
  //대장내시경
  Pcv_GumSelRptcd('C03', Qlb3_GNM64);
  //내시경
  If (Qlb3_GNM61.Color = $00FF80FF) or
     (Qlb3_GNM63.Color = $00FF80FF) or
     (Qlb3_GNM64.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM52.Transparent := False;
    Qlb3_GNM52.Color := $00FF80FF;
  End;

  //장내미생물
  Pcv_GumSelRptcd('C04', Qlb3_GNM54);
  //얼리택
  Pcv_GumSelRptcd('C05', Qlb3_GNM57);
  //모발미네랄검사
  Pcv_GumSelRptcd('C06', Qlb3_GNM60);
  //기타
  If (Qlb3_GNM54.Color = $00FF80FF) or
     (Qlb3_GNM57.Color = $00FF80FF) or
     (Qlb3_GNM60.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM58.Transparent := False;
    Qlb3_GNM58.Color := $00FF80FF;
  End;

  //흉부X선촬영
  Pcv_GumSelRptcd('D00', Qlb3_GNM15);
  //유방X선촬영
  Pcv_GumSelRptcd('D01', Qlb3_GNM18);
  //골밀도검사
  Pcv_GumSelRptcd('D02', Qlb3_GNM21);
  //뇌CT
  Pcv_GumSelRptcd('D03', Qlb3_GNM22);
  //경추CT
  Pcv_GumSelRptcd('D04', Qlb3_GNM26);
  //요추CT
  Pcv_GumSelRptcd('D05', Qlb3_GNM30);
  //심장CT
  Pcv_GumSelRptcd('D06', Qlb3_GNM32);
  //흉부CT
  Pcv_GumSelRptcd('D07', Qlb3_GNM34);
  //복부CT
  Pcv_GumSelRptcd('D08', Qlb3_GNM38);
  //방사선실
  If (Qlb3_GNM14.Color = $00FF80FF) or
     (Qlb3_GNM21.Color = $00FF80FF) or
     (Qlb3_GNM18.Color = $00FF80FF) or
     (Qlb3_GNM22.Color = $00FF80FF) or
     (Qlb3_GNM26.Color = $00FF80FF) or
     (Qlb3_GNM30.Color = $00FF80FF) or
     (Qlb3_GNM32.Color = $00FF80FF) or
     (Qlb3_GNM34.Color = $00FF80FF) or
     (Qlb3_GNM38.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM25.Transparent := False;
    Qlb3_GNM25.Color := $00FF80FF;
  End;

  //상복부초음파
  Pcv_GumSelRptcd('E00', Qlb3_GNM42);
  //하복부초음파
  Pcv_GumSelRptcd('E01', Qlb3_GNM44);
  //갑상선초음파
  Pcv_GumSelRptcd('E02', Qlb3_GNM46);
  //경동맥초음파
  Pcv_GumSelRptcd('E03', Qlb3_GNM50);
  //유방초음파
  Pcv_GumSelRptcd('E04', Qlb3_GNM51);
  //심장초음파
  Pcv_GumSelRptcd('E05', Qlb3_GNM53);
  //초음파실
  If (Qlb3_GNM42.Color = $00FF80FF) or
     (Qlb3_GNM44.Color = $00FF80FF) or
     (Qlb3_GNM46.Color = $00FF80FF) or
     (Qlb3_GNM50.Color = $00FF80FF) or
     (Qlb3_GNM51.Color = $00FF80FF) or
     (Qlb3_GNM53.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM47.Transparent := False;
    Qlb3_GNM47.Color := $00FF80FF;
  End;

  //에스테틱
  Pcv_GumSelRptcd('F00', Qlb3_GNM55);
  //영양수액
  Pcv_GumSelRptcd('F01', Qlb3_GNM59);

  //기타
  If (Qlb3_GNM55.Color = $00FF80FF) or
     (Qlb3_GNM59.Color = $00FF80FF) Then
  Begin
    Qlb3_GNM58.Transparent := False;
    Qlb3_GNM58.Color := $00FF80FF;
  End;

  //비수면
  If (Qlb3_GNM61.Color <> $00FF80FF) And
     ((Qlb3_GNM63.Color = $00FF80FF) Or (Qlb3_GNM64.Color = $00FF80FF)) Then
  Begin
    Qlb3_GNM62.Transparent := False;
    Qlb3_GNM62.Color := $00FF80FF;
  End;
  
end;


procedure TFormQRChart02.Pcv_GumSelRptcd(Is_RpSub : AnsiString; Io_CpNm1 : TQRPrintable);
var
  Ms_TblN1, Ms_FldN1 : AnsiString;
  Ms_TblN2, Ms_FldN2 : AnsiString;
  Ms_TblN3, Ms_FldN3 : AnsiString;
Begin

  If Ui_JUBGN = 0 Then  //접수
  Begin
    Ms_TblN1 := '' + As_Dmain + 'JUBSUTABLE';
    Ms_FldN1 := 'JUBS';
    Ms_TblN2 := '' + As_Dmain + 'JUBGKTABLE';
    Ms_FldN2 := 'JUBG';
    Ms_TblN3 := '' + As_Dmain + 'JUBEMTABLE';
    Ms_FldN3 := 'JUBE';
    //Qlb3_JUBGN.Caption := '(접수)';
  End Else
  If Ui_JUBGN = 1 Then  //예약
  Begin
    Ms_TblN1 := '' + As_Dmain + 'PMSSUTABLE';
    Ms_FldN1 := 'PMSS';
    Ms_TblN2 := '' + As_Dmain + 'PMSGKTABLE';
    Ms_FldN2 := 'PMSG';
    Ms_TblN3 := '' + As_Dmain + 'PMSEMTABLE';
    Ms_FldN3 := 'PMSE';
    //Qlb3_JUBGN.Caption := '(예약)';
  End;

  With ModuDbsEngine.Cds2_Qury2 Do
  Begin
    Am_11SQL.Clear; Am_11PNM.Clear; Am_11PVL.Clear;
    Am_11SQL.Text := 'Select Rpdtle.RPTCRPTNM, Rpdtle.RPTCRPSUB, Rpdtle.RPTCRPTCD   ' + #13#10 +
                '  From ' + Ms_TblN3 + ' Jumtle    ' + #13#10 +
                ' Inner Join ' + As_Dbase + 'RPTCDTABLE Rpdtle On Jumtle.'+Ms_FldN3+'PCODE = Rpdtle.RPTCRPTCD  ' + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPGBN',  '=', 'RPTCRPGB1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPTNM',  '=', 'RPTCRPTN1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPTKY', '<>', 'RPTCRPTK1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPSUB',  '=', 'RPTCRPSB1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', 'Where', 'Jumtle.'+Ms_FldN3+'JUBNO', '=', Ms_FldN3+'JUBNO', Am_11PNM)    + #13#10 +
                'Union All ' + #13#10 +
                'Select Rpdtle.RPTCRPTNM, Rpdtle.RPTCRPSUB, Rpdtle.RPTCRPTCD ' + #13#10 +
                'From ' + As_Dmain + 'RESONTABLE Rentle ' + #13#10 +
                '  Inner Join ' + As_Dbase + 'RPTCDTABLE Rpdtle On Rentle.RESOICODE = Rpdtle.RPTCRPTCD ' + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPGBN',  '=', 'RPTCRPGB1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPTNM',  '=', 'RPTCRPTN1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPTKY', '<>', 'RPTCRPTK1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPSUB',  '=', 'RPTCRPSB1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', 'Where', 'Rentle.RESOJUBNO', '=', Ms_FldN1+'JUBNO', Am_11PNM)    + #13#10 +

                'Union All ' + #13#10 +
                'Select Rpdtle.RPTCRPTNM, Rpdtle.RPTCRPSUB, Rpdtle.RPTCRPTCD ' + #13#10 +
                'From ' + As_Dmain + 'RESFFTABLE Reftle ' + #13#10 +
                '  Inner Join ' + As_Dbase + 'RPTCDTABLE Rpdtle On Reftle.RESFICODE = Rpdtle.RPTCRPTCD ' + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPGBN',  '=', 'RPTCRPGB1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPTNM',  '=', 'RPTCRPTN1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPTKY', '<>', 'RPTCRPTK1', Am_11PNM) + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', '    And', 'Rpdtle.RPTCRPSUB',  '=', 'RPTCRPSB1', Am_11PNM) + #13#10 +
                //'Where Reftle.RESFJUBNO = '''+Us_Jubno+''' ' + #13#10 +
                ModuDbsEngine.Fcb_SQLParamAd('U', 'Where', 'Reftle.RESFJUBNO', '=', Ms_FldN1+'JUBNO', Am_11PNM)                + #13#10 +

                '';
                //ModuDbsEngine.Fcb_MessageBox('01','',Am_11SQL.Text);
    ModuDbsEngine.Fcb_SQLParamVl(Am_11PVL, 'RPTCRPGB1', 'HSPTAL');
    ModuDbsEngine.Fcb_SQLParamVl(Am_11PVL, 'RPTCRPTN1', Us_RptNm);
    ModuDbsEngine.Fcb_SQLParamVl(Am_11PVL, 'RPTCRPTK1', '0');
    ModuDbsEngine.Fcb_SQLParamVl(Am_11PVL, 'RPTCRPSB1', Is_RpSub);
    ModuDbsEngine.Fcb_SQLParamVl(Am_11PVL,  Ms_FldN3+'JUBNO', Us_Jubno);
    ModuDbsEngine.Fcb_SQLQueryEx('OP', 'Y', ModuDbsEngine.Cds2_Qury2, Am_11SQL, Am_11PNM, Am_11PVL);
  End;

  If Not ModuDbsEngine.Cds2_Qury2.Eof Then
  Begin
    If (Copy(UpperCase(Io_CpNm1.Name),1,7) = 'QLB3_CK') Then
    Begin
      TQRLabel(Io_CpNm1).Caption := '√';
    End Else
    If (Copy(UpperCase(Io_CpNm1.Name),1,5) = 'QLB1_') Or
       (Copy(UpperCase(Io_CpNm1.Name),1,5) = 'QLB3_')Then
    Begin
      TQRLabel(Io_CpNm1).Transparent := False;
      TQRLabel(Io_CpNm1).Color := $00FF80FF;
    End Else
    If (Copy(UpperCase(Io_CpNm1.Name),1,5) = 'QSP3_') Or
       (Copy(UpperCase(Io_CpNm1.Name),1,5) = 'QSP1_') Then
    Begin
      TQRShape(Io_CpNm1).Brush.Color := clBlack;
    End;
  End;




End;

end.
