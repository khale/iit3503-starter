`define RAMWIDTH 16
import "DPI-C" function void extern_ram(input bit en, 
                                        input bit wEn, 
                                        input shortint dataIn, 
                                        input shortint addr, 
                                        output shortint dataOut, 
                                        output bit R);

module ExternalRAM(
  input  clk,
  input  en,
  input  wEn,
  input  [`RAMWIDTH-1:0] dataIn,
  input  [`RAMWIDTH-1:0] addr,
  output [`RAMWIDTH-1:0] dataOut,
  output R
);

  always @(posedge clk) begin
    extern_ram(en, wEn, dataIn, addr, dataOut, R);
  end

endmodule
