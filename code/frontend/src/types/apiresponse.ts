export type ApiResponse<T> =
  | {
      success: true;
      message: string;
      data: T;
      error: null;
    }
  | {
      success: false;
      message: string;
      data: null;
      error: any; // หรือ string | Record<string, any>
    };